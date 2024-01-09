// Copyright 2020 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package handler

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"net/http"
	"net/url"
	"regexp"
	"time"

	"github.com/dchest/authcookie"
	"github.com/harness/harness-core/product/platform/client"

	"github.com/harness/harness-core/product/log-service/cache"
	"github.com/harness/harness-core/product/log-service/config"
	"github.com/harness/harness-core/product/log-service/entity"
	"github.com/harness/harness-core/product/log-service/logger"
)

const authHeader = "X-Harness-Token"
const authAPIKeyHeader = "x-api-key"
const authTokenHeader = "Authorization"
const orgIdentifier = "orgId"
const projectIdentifier = "projectId"
const pipelineIdentifier = "pipelineId"

const routingIDparam = "routingId"
const regexp1 = "runSequence:[\\d+]"
const regexp2 = `\w+\/pipeline\/\w+\/[1-9]\d*\/`
const resource_pipeline = "PIPELINE"
const pipeline_view_permission = "core_pipeline_view"

// TokenGenerationMiddleware is middleware to ensure that the incoming request is allowed to
// invoke token-generation endpoints.
func TokenGenerationMiddleware(config config.Config, validateAccount bool, ngClient *client.HTTPClient) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			if validateAccount {
				accountID := r.FormValue(accountIDParam)
				if accountID == "" {
					WriteBadRequest(w, errors.New("no account ID in query params"))
					return
				}
			}

			//Get X-api-key from header, if not then check for token
			inputApiKey := r.Header.Get(authAPIKeyHeader)
			if inputApiKey != "" {
				err := doApiKeyAuthentication(inputApiKey, r.FormValue(accountIDParam), r.FormValue(routingIDparam), ngClient)
				if err != nil {
					WriteBadRequest(w, errors.New("apikey in request not authorized for receiving tokens"))
					return
				}
			} else {
				// Try to get token from the header or the URL param
				inputToken := r.Header.Get(authHeader)
				if inputToken == "" {
					inputToken = r.FormValue(authHeader)
				}

				if inputToken == "" {
					WriteBadRequest(w, errors.New("no token or x-api-key in header"))
					return
				}

				if inputToken != config.Auth.GlobalToken {
					// Error: invalid token
					WriteBadRequest(w, errors.New("token in request not authorized for receiving tokens"))
					return
				}
			}

			next.ServeHTTP(w, r)
		})
	}
}

// AuthInternalMiddleware is middleware to ensure that the incoming request is allowed for internal APIs only
func AuthInternalMiddleware(config config.Config, validateAccount bool, ngClient *client.HTTPClient) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			if validateAccount {
				accountID := r.FormValue(accountIDParam)
				if accountID == "" {
					WriteBadRequest(w, errors.New("no account ID in query params"))
					return
				}
			}

			// Try to get token from the header or the URL param
			inputToken := r.Header.Get(authHeader)
			if inputToken == "" {
				WriteBadRequest(w, errors.New("no token in header"))
				return
			}

			if inputToken != config.Auth.GlobalToken {
				// Error: invalid token
				WriteBadRequest(w, errors.New("token in request not authorized for receiving tokens"))
				return
			}

			next.ServeHTTP(w, r)
		})
	}
}

// AuthMiddleware is middleware to ensure that the incoming request is allowed to access resources
// at the specific accountID, also does ACL check incoming request is allowed to access resources
// at the specific accountID,project, org and pipeline
func AuthMiddleware(config config.Config, ngClient, aclClient *client.HTTPClient, skipKeyCheck bool) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {

			accountID := r.FormValue(accountIDParam)
			if accountID == "" {
				WriteBadRequest(w, errors.New("no account ID in query params"))
				return
			}

			inputApiKey := r.Header.Get(authAPIKeyHeader)
			inputAuthToken := r.Header.Get(authTokenHeader)
			var validated bool

			//Check if Auth token is present in Header and if method is allowed,
			//then check acl with call to access control, else check for old approach (x-api-key or X-harness-Token)
			if inputAuthToken != "" && r.Method == http.MethodGet {
				err := validateACL(r, aclClient, accountID, inputAuthToken)
				if err != nil {
					logger.FromRequest(r).
						WithError(err).
						WithField("accountID", accountID).
						Errorln("middleware: failed to validate access control")
					//Donot return here, as we need to check for old approach (x-api-key or X-harness-Token). this is for release backward compatibility. Will need to remove this later.
				} else {
					validated = true
				}
			}

			// Fallback to old approach (x-api-key or X-harness-Token) if not validated by access control
			if !validated {
				if inputApiKey != "" {
					// Try to check token from the header or the URL param
					err := doApiKeyAuthentication(inputApiKey, r.FormValue(accountIDParam), r.FormValue(routingIDparam), ngClient)
					if err != nil {
						logger.FromRequest(r).
							WithError(err).
							WithField("accountIDParam", r.FormValue(accountIDParam)).
							Errorln("middleware: apikey in request not authorized for receiving tokens")
						writeError(w, errors.New("apikey in request not authorized for receiving tokens"), 403)
						return
					}
				} else {
					inputToken := r.Header.Get(authHeader)
					if inputToken == "" {
						inputToken = r.FormValue(authHeader)
					}

					if inputToken == "" {
						WriteBadRequest(w, errors.New("no token in header"))
						return
					}
					// accountID in token should be same as accountID in URL
					secret := []byte(config.Auth.LogSecret)
					login := authcookie.Login(inputToken, secret)
					if login == "" || login != accountID {
						writeError(w, errors.New(fmt.Sprintf("operation not permitted for accountID: %s", accountID)), 403)
						return
					}
				}
			}

			// Validate that a key field is present in the request
			if !skipKeyCheck && r.FormValue(keyParam) == "" {
				WriteBadRequest(w, errors.New("no key exists in the URL"))
				return
			}

			next.ServeHTTP(w, r)
		})
	}
}

func validateACL(r *http.Request, aclClient *client.HTTPClient, accountID, inputAuthToken string) error {

	if r.FormValue(pipelineIdentifier) == "" || r.FormValue(projectIdentifier) == "" || r.FormValue(orgIdentifier) == "" {
		return errors.New("scope pipelineId, projectId and orgId are required for validating access")
	}

	allowed, err := aclClient.ValidateAccessforPipeline(r.Context(), inputAuthToken, accountID, r.FormValue(pipelineIdentifier), r.FormValue(projectIdentifier), r.FormValue(orgIdentifier), resource_pipeline, pipeline_view_permission)
	if err != nil {
		return fmt.Errorf("error validating access for resource, unauthorized or expired token %w", err)
	}
	if !allowed {
		return errors.New("user not authorized")
	}

	return nil
}

func doApiKeyAuthentication(inputApiKey, accountID, routingId string, ngClient *client.HTTPClient) error {
	err := ngClient.ValidateApiKey(context.Background(), accountID, routingId, inputApiKey)
	return err
}

func CacheRequest(c cache.Cache) func(handler http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			ctx := r.Context()
			logger.WithContext(ctx, logger.FromRequest(r))

			var info entity.ResponsePrefixDownload
			prefix := r.URL.Query().Get(usePrefixParam)
			exists := c.Exists(ctx, prefix)

			if exists {
				logger.FromRequest(r).Infoln("Request found in cache for prefix", prefix)
				inf, err := c.Get(ctx, prefix)
				if err != nil {
					logger.FromRequest(r).
						WithError(err).
						WithField("url", r.URL.String()).
						WithField("Prefix", prefix).
						WithField("time", time.Now().Format(time.RFC3339)).
						Errorln("middleware cache: cannot get prefix")
					WriteInternalError(w, err)
					return
				}

				err = json.Unmarshal(inf, &info)
				if err != nil {
					logger.FromRequest(r).
						WithError(err).
						WithField("url", r.URL.String()).
						WithField("time", time.Now().Format(time.RFC3339)).
						WithField("Prefix", prefix).
						WithField("info", inf).
						Errorln("middleware cache: failed to unmarshal info")
					WriteInternalError(w, err)
					return
				}

				switch info.Status {
				case entity.QUEUED:
				case entity.IN_PROGRESS:
					logger.FromRequest(r).Infoln("Returning queued or inprogress for prefix", prefix)
					WriteUnescapeJSON(w, info, 200)
					return
				case entity.ERROR:
					err := c.Delete(ctx, prefix)
					if err != nil {
						logger.FromRequest(r).
							WithError(err).
							WithField("url", r.URL.String()).
							WithField("time", time.Now().Format(time.RFC3339)).
							WithField("Prefix", prefix).
							WithField("info", inf).
							Errorln("middleware cache: failed to delete error in cache")
						WriteInternalError(w, err)
						return
					}
					logger.FromRequest(r).WithField("Prefix", prefix).Infoln("Deleted from cache")
					WriteUnescapeJSON(w, info, 200)
					return
				case entity.SUCCESS:
					logger.FromRequest(r).Infoln("Returning success found in cache for prefix", prefix)
					WriteUnescapeJSON(w, info, 200)
					return
				default:
					logger.FromRequest(r).WithField("Prefix", prefix).Infoln("info status does not match, going to default")
					next.ServeHTTP(w, r)
					return
				}
			}
			logger.FromRequest(r).Infoln("Prefix does not exist in cache as it is first attempt", prefix)
			next.ServeHTTP(w, r)
			return
		})
	}
}

func RequiredQueryParams(params ...string) func(handler http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			for _, param := range params {
				value := r.FormValue(param)
				if len(value) == 0 || value == "" {
					err := errors.New(fmt.Sprintf("parameter %s is required.", param))
					WriteNotFound(w, err)
					logger.FromRequest(r).
						WithField("url", r.URL.String()).
						WithField("time", time.Now().Format(time.RFC3339)).
						WithError(err).
						Errorln("middleware validate query params: doesnt contain query param", param)
					return
				}
			}
			next.ServeHTTP(w, r)
		})
	}
}

func ValidatePrefixRequest() func(handler http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			unescapedUrl, err := url.QueryUnescape(r.URL.String())
			if err != nil {
				WriteInternalError(w, err)
				logger.FromRequest(r).
					WithField("url", r.URL.String()).
					WithField("time", time.Now().Format(time.RFC3339)).
					WithError(err).
					Errorln("middleware validate: cannot match execution in prefix")
				return
			}

			containRunSequence, err := regexp.MatchString(regexp1, unescapedUrl)
			if err != nil {
				WriteInternalError(w, err)
				logger.FromRequest(r).
					WithField("url", r.URL.String()).
					WithField("time", time.Now().Format(time.RFC3339)).
					WithError(err).
					Errorln("middleware validate: cannot match execution in prefix")
				return
			}

			regex := regexp.MustCompile(regexp2)
			containRunSequenceForSimplifiedLogBaseKey := regex.MatchString(unescapedUrl)

			if containRunSequence || containRunSequenceForSimplifiedLogBaseKey {
				logger.WithContext(context.Background(), logger.FromRequest(r))
				logger.FromRequest(r).
					WithField("url", r.URL.String()).
					WithField("time", time.Now().Format(time.RFC3339)).
					Debug("middleware validate: contain execution in prefix")
				next.ServeHTTP(w, r)
				return
			}

			err = errors.New(fmt.Sprintf("operation not permitted for prefix: %s", r.URL.String()))
			WriteBadRequest(w, err)
			logger.FromRequest(r).
				WithField("url", r.URL.String()).
				WithField("time", time.Now().Format(time.RFC3339)).
				WithError(err).
				Errorln("middleware validate: doesnt contain execution in prefix")
			return
		})
	}
}
