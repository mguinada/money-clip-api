(ns money-clip.persistence.users-test
  (:require [clojure.test :refer [deftest testing is] :as t]
            [clojure.spec.test.alpha :as st]
            [clojure.string :as str]
            [money-clip.system :as system]
            [money-clip.persistence.users :as users]
            [money-clip.model.user :as u]))

(st/instrument)

(t/use-fixtures :once system/init)
(t/use-fixtures :each system/cleanup)

(deftest create-user-test
  (let [user (u/new-user "john.doe@doe.net" "pa66w0rd" "John" "Doe")]
    (testing "when the user's email is not already in use"
      (is (= "john.doe@doe.net" (::u/email (users/create-user @system/db user "pa66w0rd")))))
    (testing "when the user's password and it's confirmation do not match"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Passwords don't match" (users/create-user @system/db user "does-not-match"))))
    (testing "when the user's email is already in use"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Email already taken"
                            (users/create-user @system/db user "pa66w0rd")) "If the case matches")
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Email already taken"
                            (users/create-user @system/db (update user ::u/email str/upper-case) "pa66w0rd")) "If the case does not match"))))

(deftest find-user-by-id-test
  (testing "when the user with the given id exists"
    (let [new-user (u/new-user "john.doe@doe.net" "pa66w0rd" "John" "Doe")
          user (users/create-user @system/db new-user "pa66w0rd")]
      (is (= user (users/find-user-by-id @system/db (::u/id user))))))
  (testing "when the user with the given id doesn't exist"
    (is (nil? (users/find-user-by-id @system/db 1)))))

(deftest find-user-by-email-test
  (testing "when the user with the given email exists"
    (let [new-user (u/new-user "john.doe@doe.net" "pa66w0rd" "John" "Doe")
          user (users/create-user @system/db new-user "pa66w0rd")]
      (is (= user (users/find-user-by-email @system/db "john.doe@doe.net")) "If the case matches")
      (is (= user (users/find-user-by-email @system/db "John.Doe@doe.net")) "If the case doesn't match")))
  (testing "when the user with the given email doesn't exist"
    (is (nil? (users/find-user-by-email @system/db "jane.doe@doe.net")))))

(deftest authenticate-user-test
  (let [user (users/create-user @system/db (u/new-user "john.doe@doe.net" "pa66w0rd" "John" "Doe") "pa66w0rd")
        _ (users/create-user @system/db (u/new-user "jane.doe@doe.net" "pa66w0rd" "John" "Doe" false) "pa66w0rd")]
    (testing "when the email and password are valid credentials"
      (is (= (dissoc user ::u/password) (users/authenticate-user @system/db "john.doe@doe.net" "pa66w0rd"))) "Authenticates the user")
    (testing "when a user with a given email is not found"
      (is (nil? (users/authenticate-user @system/db "james.doe@doe.net" "pa66w0rd"))) "Returns nil")
    (testing "when a user with a given email is found but the passwors is invalid"
      (is (nil? (users/authenticate-user @system/db "john.doe@doe.net" "invalid-passwd"))) "Returns nil")
    (testing "when the email and password are valid credentials yet the user is not active"
      (is (nil? (users/authenticate-user @system/db "jane.doe@doe.net" "pa66w0rd"))) "Returns nil")))

(deftest update-user
  (testing "when the update is successful"
    (let [user (users/create-user @system/db (u/new-user "john.doe@doe.net" "pa66w0rd" "John" "Doe") "pa66w0rd")
          updated-user (users/update-user @system/db (assoc user ::u/last-name "Don" ::u/first-name "Joe"))]
      (is (= "Don" (::u/last-name updated-user)) "Updates the last name")
      (is (= "Joe" (::u/first-name updated-user)) "Updates the first name")
      (is (not= (::u/created-at updated-user) (::u/updated-at updated-user)) "Touches the update timestamp")))
  (testing "when the update is not successful"
    (let [transient-user (u/new-user "jane.doe@doe.net" "pa66w0rd" "Jane" "Doe")]
      (is (nil? (users/update-user @system/db (assoc transient-user ::u/id 123 ::u/first-name "Mary")))))))

(deftest update-user-password
  (let [user (users/create-user @system/db (u/new-user "john.doe@doe.net" "pa66w0rd" "John" "Doe") "pa66w0rd")]
    (testing "when the current password and the password confirmation are both valid"
      (is (= (::u/id user) (::u/id (users/update-user-password @system/db user "pa66w0rd" "Pa66wOrd" "Pa66wOrd")))))
    (testing "when the current password is invalid"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Authentication failed" (users/update-user-password @system/db user "wrong-pa66w0rd" "Pa66wOrd" "Pa66wOrd"))))
    (testing "when the password confirmation does not match the password"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Passwords don't match" (users/update-user-password @system/db user "Pa66wOrd" "Pa66wOrd" "password"))))))
