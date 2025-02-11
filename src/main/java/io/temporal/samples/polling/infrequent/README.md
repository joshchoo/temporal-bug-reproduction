## Infrequent polling

This sample shows how we can use activity retries for infrequent polling of
a third-party service (for example via REST). 
This method can be used for infrequent polls of one minute or slower.

We utilize activity retries for this option, setting Retries options:
* setBackoffCoefficient to 1
* setInitialInterval to the polling interval (in this sample set to 60 seconds)

This will allow us to retry our activity exactly on the set interval.

To run this sample: 
```bash
./gradlew -q execute -PmainClass=io.temporal.samples.polling.infrequent.Starter
```

Since our test service simulates it being "down" for 4 polling attempts and then
returns "OK" on the 5th poll attempt, our workflow is going to perform 
4 activity retries with a 60 second poll interval, and then return 
the service result on the successful 5th attempt. 

Note that individual activity retries are not recorded in workflow history,
so we this approach we can poll for a very long time without
affecting the history size.

