(ns money-clip.handler.users
  (:require [ataraxy.response :as response]
            [integrant.core :as ig]
            [buddy.sign.jwt :as jwt]
            [tick.core :as t]
            [money-clip.utils :as ut]
            [money-clip.persistence.users :as users]
            [money-clip.model.user :as u]))

(defmethod ig/init-key ::create [_ {:keys [db]}]
  (fn [{[_ email password first-name last-name] :ataraxy/result}]
    (let [user (users/create-user db (u/new-user email password first-name last-name))]
      [::response/created (str "/users/" (::u/id user)) {:user (-> user (dissoc ::u/password) ut/unqualify-keys)}])))

(defmethod ig/init-key ::login [_ {:keys [db jwt-secret]}]
  (letfn [(sign-token [user]
                      (->>
                       {:exp (t/>> (t/now) (t/new-period 1 :days))}
                       (jwt/sign (select-keys user [::u/email]) jwt-secret)
                       (assoc user ::u/auth-token)))]
    (fn [{[_ email password] :ataraxy/result}]
      (if-let [user (users/authenticate-user db email password)]
        [::response/ok {:user (ut/unqualify-keys (sign-token user))}]
        [::response/forbidden "Not authorized"]))))