# KokuminIPChecker
IP リストを HTTP 経由で投げると1分以内に情報を拾ってきてくれる物

レートリミット制限: 40回/分

・リクエスト
`http://127.0.0.1:53185/request?["1.1.1.1","8.8.8.8"]`

・レスポンス
```Json
{
    "success": true,
    "data": [
        {
            "query": "1.1.1.1",
            "country": "AU",
            "region": "Queensland",
            "mobile": false,
            "proxy": false,
            "hosting": true
        },
        {
            "query": "8.8.8.8",
            "country": "US",
            "region": "Virginia",
            "mobile": false,
            "proxy": false,
            "hosting": true
        }
    ]
}
```

# Maven
- Repository
```XML
  <repositories>
    <repository>
      <id>net.simplyrin</id>
      <name>api</name>
      <url>https://api.simplyrin.net/maven/</url>
    </repository>
  </repositories>
```

- Dependency
```XML
  <dependencies>
    <dependency>
      <groupId>net.simplyrin.kokuminipchecker</groupId>
      <artifactId>KokuminIPChecker</artifactId>
      <version>1.2</version>
    </dependency>
  </dependencies>
```
