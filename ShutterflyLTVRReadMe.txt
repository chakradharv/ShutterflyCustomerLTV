
Code Flow
ingestData method accepts array of strings as input.It iterates over each string converts json string to json object using json parser.
If type is CUSTOMER ,I am preparing customerid, last name map. we can also update customer info if verb type is update.
If the type is SITE_VISIT I am preparing map which consists of customerid as key
and the value is map where key is week number and value is number of times user has visited that site in a week.
If type equals ORDER I am preparing map which consists of customerid as key where value is map where key is orderid and
value is total amount for that order, we can also update the order amount by using order id for particular order if verb type is update

topXSimpleLTVCustomers method accepts X ,I am building custIdLTVMap from customerIdWeekCountMap and customerIdOrderIdAmountMap.
I am iterating over customerIdWeekCountMap and preparing map where key is customerid and value is average of site visits within a week.
Then I am calculating the average of order amount for each customers using customerIdOrderIdAmountMap map.
Then I am preparing custIdLTVMap where key is customerid and value is 52*a*10.
Then I am sorting custIdLTVMap using values and returning top x customers

Here I am assuming the data of only one year.

Performance:
As I am suing Maps for storing retrieval from map taken O(1) time.
For sorting Map for getting topX customers takes O(nlongn) time where n is number of customers.

In the future If the data is huge the performance of this can be improved by using spark by preparing dataframes and using joins.
