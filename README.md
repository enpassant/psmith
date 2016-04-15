# psmith
Simple router and load balancer for (REST) microservices

## Fetaures

* Routing: forward the event for the appropriate micro service server
* Metrics: measure and calculate many information about the communications:
  * started, success and failed requests counter
  * min, max and mean time
  * stdDev and oneMinuteRate
* Adaptive Load balancing: distributes workloads across multiple micro service server based on metrics
* Caching (REST): cache all HTTP read methods (GET, HEAD, OPTIONS), and clear cache at write methods
* Tagging: mark the micro service server with a tag for special handling (e.g. developing, testing)

## Technologies

* Akka actors
* Akka-http
* Spray caching
* Google ConcurrentLinkedHashMap
* nscala-time
* Json4s (jackson)
* metrics-scala
