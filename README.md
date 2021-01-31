### MatchingEngine

##### Simple Implementation of a matching engine which is used to market and limit orders
##### MatchingEngine Behaviour 
  - It support market and limit order
  - Currently product attributes are constant
  - It sorts both incoming order based on Price and EventTime .
  - It generates Trades only for orders
  - It has isMatchingOn switch to stop matching . During this time we can allow posting order/cancel/amend orders
  - Validation for lotsize, ticksize and some basic checks has been added(Can be improved to source static data)
  - Support Amend/Cancel of the orders
  
##### DESIGN

 - The implementation uses a priority queue where we always ensure that the buy and sell orders are kept in the queue as per the buy/sell comparator
 - Currently as it doesnt take into MD data , so matching is triggered only on order events
 - Validation for lotsize, ticksize and some basic checks has been added(Can be improved further) 
 - Only quantity amend down results in queue priority being retained
 - Price amend as well as only quantity amend up and quantity plus price amend up leads to queue priority being lost.
 - PoolManager has been used to ensure the pool is preallocated at startup and no gc happens during runtime

##### Improvement
   - Currently priority queue is not GC free, come up with datastructure which is GC free(Mainly Iterator as it creates new object each time)
   - Order can we move to fixed offset based object , so the access and storage can be more efficient   
##### How to run      
   - In mvn  environment just run "mvn clean build" 
   
   - Log level is ERROR by default . Configured in "MatchingEngine\src\test\resources\log4j.properties"
   
   - "testLoadTesting" method is added to demonstrate gc . This test is sensitive to heap size both young and old generation. Idea we need to do load testing and size the heap during the preallocated capacity.We need to make sure they is no logs created for every order(as string in java used in log4j created new objects)
   
   - In case if we want to change the log level to INFO or DEBUG, it is suggested to disable the "testLoadTesting" as log4j will create garbage which will make test case as Minor GC will not be zero