(ns movie-review.tools.tmdb
  "TMDB (The Movie Database) API integration for movie search and details."
  (:require [clj-http.client :as http]
            [clojure.string :as str]
            [secrets.core]))

(defn- get-tmdb-key
  "Get TMDB API key from secrets"
  []
  (or (System/getenv "TMDB_API_KEY")
      (secrets.core/get-secret :tmdb-api-key)))

(defn- tmdb-request
  "Make a request to TMDB API"
  [endpoint params]
  (let [api-key (get-tmdb-key)]
    (when-not api-key
      (throw (ex-info "TMDB API key not found"
                      {:hint "Set TMDB_API_KEY env var or add :tmdb-api-key to secrets.edn"})))
    (let [url (str "https://api.themoviedb.org/3" endpoint)
          response (http/get url
                             {:query-params (merge {:api_key api-key} params)
                              :as :json
                              :throw-exceptions false})]
      (if (= 200 (:status response))
        (:body response)
        (throw (ex-info "TMDB API error"
                        {:status (:status response)
                         :body (:body response)}))))))

(defn search-movie
  "Search for a movie by name"
  [query]
  (let [response (tmdb-request "/search/movie" {:query query :language "en-US"})
        results (:results response)]
    (if (empty? results)
      {:status :empty
       :query query
       :text (str "No movies found for: " query)}
      {:status :ok
       :query query
       :count (count results)
       :results results
       :text (str "Found " (count results) " movies matching \"" query "\"")})))

(defn get-movie-details
  "Get detailed information about a movie by ID"
  [movie-id]
  (tmdb-request (str "/movie/" movie-id) {:language "en-US"}))

(defn get-movie-reviews
  "Get reviews for a movie by ID"
  [movie-id]
  (let [response (tmdb-request (str "/movie/" movie-id "/reviews") {:language "en-US"})]
    (:results response)))

(defn tmdb-movie-report
  "Search for a movie and generate a comprehensive report with details and reviews.
   
   Args:
     :message OR :query → movie name to search for
   
   Returns observation with:
     :status :ok or :empty
     :text → formatted report with movie info and reviews"
  [{:keys [message query] :as _args}]
  (let [movie-name (or query message)]
    (when (str/blank? movie-name)
      (throw (ex-info "tmdb-movie-report requires a :query or :message" {})))

    ;; Search for the movie
    (let [search-result (search-movie movie-name)]
      (if (= :empty (:status search-result))
        search-result
        ;; Get the first movie with actual data (non-zero votes)
        (let [movie (or (first (filter #(> (:vote_count % 0) 0) (:results search-result)))
                        (first (:results search-result)))  ; fallback to first if none have votes
              movie-id (:id movie)
              details (get-movie-details movie-id)
              reviews (get-movie-reviews movie-id)

              ;; Format the report
              release-year (let [date (:release_date details)]
                             (if (and date (>= (count date) 4))
                               (subs date 0 4)
                               "Unknown"))
              report (str/join "\n\n"
                               ["# " (:title details) " (" release-year ")"
                                ""
                                "## Overview"
                                (:overview details)
                                ""
                                (str "**Rating:** " (:vote_average details) "/10 (" (:vote_count details) " votes)")
                                (str "**Genres:** " (str/join ", " (map :name (:genres details))))
                                (str "**Runtime:** " (:runtime details) " minutes")
                                ""
                                "## User Reviews"
                                ""
                                (if (empty? reviews)
                                  "No reviews available."
                                  (str/join "\n\n---\n\n"
                                            (map (fn [review]
                                                   (str "**" (:author review) "** (Rating: "
                                                        (get-in review [:author_details :rating] "N/A")
                                                        "/10)\n\n"
                                                        (let [content (:content review)]
                                                          (if (> (count content) 500)
                                                            (str (subs content 0 500) "...")
                                                            content))))
                                                 (take 3 reviews))))])]
          {:status :ok
           :movie-id movie-id
           :title (:title details)
           :rating (:vote_average details)
           :text report})))))