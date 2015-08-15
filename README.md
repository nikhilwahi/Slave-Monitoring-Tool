# **Slave-Monitoring-Tool**

## Description
Tool to monitor health of the slaves in a replicated MySQL database

## Detailed Working
There is a cron which runs every minute and checks the health of the slaves. This data is stored in a Redis database with the key being the name of the slave and the value being parameters that monitor slave health along with the timestamp. The value is in the form of JSON string. The value can also be an error message if there was an error when the cron ran. 

A web service makes a call to the Redis database and returns the value associated with the key (server name) that has been requested. The response can either include all of the data or specific data, depending on the HTTP request. If the data in the Redis database is older than 5 minutes, an error message is sent along with the data requested.



## Successful Calls

•To get all the data associated with a key (server name).
•Format: http://localhost:8081/service/webapi/monitor/server_name

Example:
http://localhost:8081/service/webapi/monitor/vm2

    output:
		{
 			"time": "11:08:01 2015-07-15",
  			"Slave_SQL_Running": "No",
  			"Seconds_Behind_Master": "NULL",
  			"error": false,
  			"Slave_IO_Running": "No",
  			"Exec_Master_Log_Pos": "1053530519"
		}

***

•To get specific data associated with a key (server name).

Format: http://localhost:8081/service/webapi/monitor/server_name?values=arg1&arg2...  

Arguments are separated by '&'

Example:
http://localhost:8081/service/webapi/monitor/vm2?values=seconds_behind_master&slave_sql_running

    output:	
		{
  			"time": "11:07:02 2015-07-15",
			"Slave_SQL_Running": "No",
  			"error": false,
  			"Seconds_Behind_Master": "NULL"
		}	


## Unsuccessful Calls

•When invalid server name is entered

Example:
http://localhost:8081/service/webapi/monitor/wrong_name

    output:
		{		
  			"error_message": "invalid server name",
  			"error": true
			"error_number" : 101
		}


***

•When all arguments are invalid

Example:
http://localhost:8081/service/webapi/monitor/vm2?values=seconds_behind_master&slave_sql_running

	output:	
  		{
	  		"error_message": "all arguments are invalid",
 		 	"error": true
			"error_number" : 105
		}	

***

•When Redis connection fails

Example:
http://localhost:8081/service/webapi/monitor/vm2?values=seconds_behind_master&slave_sql_running

	output:	
  		{
  			"error_message": "java.net.ConnectException: Connection refused redis error",
  			"error": true
			"error_number" : 104
		}

***

•When Redis data is old(older than 300 seconds) (This implies that something went wrong with the cron. Either the cron stopped working, the mysql database connection took more than 300 seconds, the Redis server was down or there was a syntax error in the json file from which server credentials are read)

Example:
http://localhost:8081/service/webapi/monitor/vm2

	output:	
  		{
 		 	"error_message": "data is old",
  			"time": "11:20:01 2015-07-15",
  			"Slave_SQL_Running": "No",
  			"Seconds_Behind_Master": "NULL",
  			"error": true,
  			"Slave_IO_Running": "No",
  			"Exec_Master_Log_Pos": "1053530519"
			"error_number" : 103
		}

***

•When the cron could not connect to the mysql server.(This may be because the server is down, or the credentials in the json file were incorrect)	
The exact error from the exception is given in the message.

Example:
http://localhost:8081/service/webapi/monitor/vm2

	output:	
  		{
  			"error_message": "Access denied for user ‘temp@192.168.1.1’ (using password: YES)",
  			"time": "11:28:48 2015-07-15",
  			"error": true
			"error_number" : 102 
		}


## Error Numbers

101: "invalid server name"

103: "old data"

104: "redis error"

105: "invalid arguments"

SQL error numbers are the standard MySQL error numbers.

Complete error messages are displayed along with the error number.
