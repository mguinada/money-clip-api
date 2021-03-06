(ns money-clip.handler.restful.resources-test
  (:require
   [clojure.test :refer [deftest is]]
   [money-clip.model.user :as u]
   [money-clip.model.bank-account :as ba]
   [money-clip.handler.restful.resources :as r]))

(def timestamp (java.util.Date.))

(deftest user-resource-test
  (let [user (u/user 1 "john.doe@doe.net" "pa66w0rd" "John" "Doe" true timestamp timestamp)]
    (is (= {:user
            {:id 1
             :email "john.doe@doe.net"
             :first-name "John"
             :last-name "Doe"
             :created-at timestamp
             :updated-at timestamp
             :_links {:self "/user" :bank-accounts "/bank-accounts"}}}
           (r/user-resource user)))))

(deftest bank-account-resource-test
  (let [user (u/user 1 "john.doe@doe.net" "pa66w0rd" "John" "Doe" true timestamp timestamp)
        bank-account (ba/bank-account 1 user "Daily expenses" "IBANK" timestamp timestamp)]
    (is (= {:bank-account
            {:id 1
             :name "Daily expenses"
             :bank-name "IBANK"
             :created-at timestamp
             :updated-at timestamp
             :_links {:self "/bank-accounts/1" :user "/user"}}}
           (r/bank-account-resource bank-account)))))
