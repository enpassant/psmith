# psmith
Simple router and load balancer for (REST) microservices

## Fetaures

* Routing: forward the event for the appropriate micro service server
* Load balancing: distributes workloads across multiple micor service server
* Caching (REST): cache all HTTP read methods (GET, HEAD, OPTIONS), and clear cache at write methods
* Tagging: mark the micro service server with a tag for special handling (e.g. developing, testing)

