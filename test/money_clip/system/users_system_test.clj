(ns money-clip.system.users-system-test
  (:require [clojure.test :refer [deftest testing is] :as t]
            [money-clip.http :as http :refer [POST PUT GET]]
            [money-clip.system :as system]
            [money-clip.utils :as ut]
            [clojure.spec.test.alpha :as st]))

(st/instrument)

(t/use-fixtures :once system/init)
(t/use-fixtures :each system/cleanup)

(deftest post-users-test
  (testing "when the email is not yet taken and the password and its confirmation match"
    (let [response (POST @system/app "/users" {:email "john.doe@doe.net"
                                               :password "pa66word"
                                               :password-confirmation "pa66word"
                                               :first-name "John"
                                               :last-name "Doe"})]
      (is (= 201 (http/status response)) "Serves a 201 HTTP status code")
      (is (= "john.doe@doe.net" (http/body response :user :email)) "Serves the user")))
  (testing "when the email is already taken"
    (let [response (POST @system/app "/users" {:email "john.doe@doe.net"
                                               :password "pa66word"
                                               :password-confirmation "pa66word"
                                               :first-name "John"
                                               :last-name "Doe"})]
      (is (= 412 (http/status response)) "Serves a 412 HTTP status code")
      (is (= "Email already taken" (http/body response :error :message)) "Returns an error message")))
  (testing "when the password and the password confirmation don't match"
    (let [response (POST @system/app "/users" {:email "john.doe@doe.net"
                                               :password "pa66word"
                                               :password-confirmation "no-match"
                                               :first-name "John"
                                               :last-name "Doe"})]
      (is (= 412 (http/status response)) "Serves a 412 HTTP status code")
      (is (= "Passwords don't match" (http/body response :error :message)) "Serves an error message"))))

(deftest post-login-test
  (let [_ (POST @system/app "/users" {:email "john.doe@doe.net"
                                      :password "pa66word"
                                      :password-confirmation "pa66word"
                                      :first-name "John"
                                      :last-name "Doe"})]
    (testing "when the login is valid"
      (let [response (POST @system/app "/login" {:email "john.doe@doe.net" :password "pa66word"})]
        (is (= 200 (http/status response)) "Serves a 200 HTTP status code")
        (is (ut/not-blank? (http/body response :user :auth-token)) "Returns an authorization token")))
    (testing "when the login is invalid"
      (let [response (POST @system/app "/login" {:email "john.doe@doe.net" :password "wrong-password"})]
        (is (= 401 (http/status response)) "Serves a 401 HTTP status code")
        (is (= "Unauthorized" (http/body response :error :message)) "Serves an error message")))))

(deftest get-user-test
  (let [[user auth-token] (system/create-user-and-login @system/app)]
    (testing "when the user is authenticated"
      (let [response (GET @system/app "/user" {} :headers {"Authorization" (str "Token " auth-token)})]
        (is (= 200 (http/status response)) "Serves a 200 HTTP status code")
        (is (= user (http/body response :user)) "Returns the user")))
    (testing "when the user is not authenticated"
      (let [response (GET @system/app "/user" {} :headers {"Authorization" "Token invalid-token"})]
        (is (= 401 (http/status response)) "Serves a 401 HTTP status code")
        (is (nil? (http/body response :user)) "Does not serve a user")))))

(deftest put-change-password
  (let [[{email :email} auth-token password] (system/create-user-and-login @system/app)
        new-password "new-pa66w0rd"]
    (testing "when the current password is invalid"
      (let [response (PUT @system/app "/user/change-password" {:current-password "invalid-password" :password new-password :password-confirmation new-password} :headers {"Authorization" (str "Token " auth-token)})]
        (is (= 412 (http/status response)) "Serves a 412 HTTP status code")
        (is (= {:error {:data {:attribute "current-password"} :message "Authentication failed"}} (http/body response)) "Serves an error")))
    (testing "when the password confirmation does not match the password"
      (let [response (PUT @system/app "/user/change-password" {:current-password password :password new-password :password-confirmation "not-a-match"} :headers {"Authorization" (str "Token " auth-token)})]
        (is (= 412 (http/status response)) "Serves a 412 HTTP status code")
        (is (= {:error {:data {:attribute "password-confirmation"} :message "Passwords don't match"}} (http/body response)) "Serves an error")))
    (testing "when the current password and new password confirmation are valid"
      (let [response (PUT @system/app "/user/change-password" {:current-password password :password new-password :password-confirmation new-password} :headers {"Authorization" (str "Token " auth-token)})]
        (is (= 200 (http/status response)) "Serves a 200 HTTP status code")
        (is (= 200 (http/status (POST @system/app "/login" {:email email :password new-password}))) "The user is able to login with the new password")))))
