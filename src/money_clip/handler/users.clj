(ns money-clip.handler.users
  (:require [ataraxy.response :as response]
            [integrant.core :as ig]
            [buddy.sign.jwt :as jwt]
            [tick.core :as t]
            [money-clip.persistence.users :as users]
            [money-clip.model.user :as u]
            [money-clip.errors :as e]
            [money-clip.handler.restful.rest :refer [defresource]]))


(defresource user :exclude [:password])

(defmethod ig/init-key ::create [_ {:keys [db]}]
  (fn [{[_ email password password-confirmation first-name last-name] :ataraxy/result}]
    (let [user (users/create-user db (u/new-user email password first-name last-name) password-confirmation)]
      [::response/created (str "/users/" (::u/id user)) (user-resource user)])))

(defmethod ig/init-key ::login [_ {:keys [db jwt-secret]}]
  (letfn [(sign-token [user]
                      (->>
                       {:exp (t/>> (t/now) (t/new-period 1 :days))}
                       (jwt/sign (select-keys user [::u/id ::u/email]) jwt-secret)
                       (assoc user ::u/auth-token)))]
    (fn [{[_ email password] :ataraxy/result}]
      (if-let [user (users/authenticate-user db email password)]
        [::response/ok (user-resource (sign-token user))]
        [::response/unauthorized e/unautorized]))))
