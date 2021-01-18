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
