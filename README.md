## Usage
This software has
```shell
mvn package && java -server -jar target/CPUIS.jar
```
Using -Dserver.port property to modify server port(default 8899).

Modify application-data.properties to customize your mongoDB configuration(host or username etc.).

maven 打包默认会加上前端，需要手动排除前端
