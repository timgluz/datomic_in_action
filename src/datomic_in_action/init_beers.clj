(ns datomic-in-action.init-beers
  (:require [datomic.api :refer [q db] :as d]
            [clojure-csv.core :refer [parse-csv] :as csv]
            [clojure.pprint :refer [pprint]]
            [taoensso.timbre :refer [debug,error] :as timbre]))

(def schema-folder "resources/schemas/")
(def schemas #{"beer-schema" "beer-category-schema" 
               "beer-style-schema" "brewery-schema"})

(def data-folder "resources/data/")
(def csv-files #{"beer.csv" "styles.csv" "categories.csv" 
                 "breweries.csv" "geocodes.csv"})

;; database uris
(def mem-uri "datomic:mem://beers")
(def free-uri "datomic:free://127.0.0.1:4334/beers")

;; create database & make connection
(d/delete-database mem-uri)
(d/create-database mem-uri)
(def conn (d/connect mem-uri))

(defn connect [uri]
  (d/connect uri))
;;parse and submit schemas

(defn import-schemas [conn schemas]
  "Reads schemas from resources folder and loads them to current database"
  (let [parse-schema (comp read-string slurp)
        to-db (partial d/transact conn)]
    (debug "Loading schemas into database.")
    (doall
      (for [schema schemas]
        (-> 
          (str schema-folder schema ".edn")
          parse-schema
          to-db ;;it returns future; we need to use ref to get value
          ref
          )))))

(import-schemas conn schemas)
;; parse and submit seed data

(def read-csv (comp parse-csv slurp))

(def table-codes {:beer 1
                  :beer-category 2
                  :beer-style 3
                  :brewery 4})

(defn id->tempid [table-nr id]
  (let [neg-int (comp - int)]
    (d/tempid 
      :db.part/user 
      (neg-int
         (+ id 
            (* table-nr 1e6))))))

(defn geocode->datom [row]
  (let [[_ brewery-id lat lng _] row
        table-nr (get table-codes :brewery)
        temp-id (id->tempid table-nr (read-string brewery-id))]
    {:db/id temp-id
     :brewery/lat (read-string lat)
     :brewery/lng (read-string lng)}))

(defn brewery->datom [row]
  (let [[brewery-id the-name,address,_,city,state,code,country,phone,
         website,_,description,_,_ ] row
        temp-id (id->tempid (:brewery table-codes) 
                            (read-string brewery-id))]
    {:db/id temp-id
     :brewery/id (read-string brewery-id)
     :brewery/name the-name
     :brewery/address address
     :brewery/city city
     :brewery/state state
     :brewery/code code
     :brewery/country country
     :brewery/phone phone
     :brewery/website website
     :brewery/description description}))


(defn beer-category->datom [row]
  (let [[cat-id,cat-name] row
        temp-id (id->tempid (:beer-category table-codes) 
                            (read-string cat-id))]
    {:db/id temp-id
     :beer.category/id (read-string cat-id)
     :beer.category/name cat-name}))

(defn beer-style->datom [row]
  (let [[id _ style-name _] row
        temp-id (id->tempid (:beer-style table-codes)
                            (read-string id))]
    {:db/id temp-id
     :beer.style/id (read-string id)
     :beer.style/name style-name}))

(defn beer->datom [row]
  (let [[id,brewery-id,the-name,cat-id,style-id,abv,ibu,srm,upc,
         filepath,descript,add_user,last_mod] row
        temp-id (id->tempid (:beer table-codes)
                             (read-string id))]
    {:db/id temp-id
     :beer/brewery (id->tempid (:brewery table-codes)(read-string brewery-id))
     :beer/name the-name
     :beer/category (id->tempid (:beer-category table-codes)(read-string cat-id))
     :beer/style (id->tempid (:beer-style table-codes)(read-string style-id))
     :beer/abv (read-string abv)
     :beer/ibu (read-string ibu)
     :beer/srm (read-string srm)
     :beer/upc (read-string upc)
     :beer/description descript}))

(defn rows->datom [mapper filename]
  (let [file-uri (str data-folder filename)
        geocodes (parse-csv (slurp file-uri))]
    (doall
      (map #(mapper %) (rest geocodes)))))

(def mapper-files [[geocode->datom "geocodes.csv"]
                   [brewery->datom "breweries.csv"]
                   [beer-category->datom "categories.csv"]
                   [beer-style->datom "styles.csv"]
                   [beer->datom "beers.csv"]])

(defn import-files [conn mapper-files]
  (let [to-db (partial d/transact conn)]
    (debug "Importing seed data")
    (doall
      (map (fn [[mapper filename]] 
              (to-db (rows->datom mapper filename))) 
           mapper-files))))

(import-files conn mapper-files)
