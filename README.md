Working implementation of the IoT [Akka Example](https://doc.akka.io/docs/akka/current/typed/guide/tutorial.html)

Includes HTTP endpoints for writing to a device's temperature, reading a device's temperature and listing all device actors.

Start a device actor
~~~
curl --request GET \
  --url http://localhost:8080/group/1/device/1
~~~
Send temperature data
~~~
curl --request POST \
  --url http://localhost:8080/group/1/device/1/data \
  --header 'content-type: application/json' \
  --data '{
	"value" : 24.23
}'
~~~
Read temperature data
~~~
curl --request GET \
  --url http://localhost:8080/group/1/device/1/data \
  --header 'content-type: application/json'
~~~
List all active actors
~~~
curl --request GET \
  --url http://localhost:8080/group/1/all
~~~