## 1. 接口描述

接口请求域名： asr.tencentcloudapi.com 。

本接口用于对语音流进行准实时识别，通过异步回调来返回识别结果。适用于直播审核等场景。  
• 支持rtmp、rtsp等流媒体协议，以及各类基于http协议的直播流(不支持hls)  
• 音频流时长无限制，服务会自动拉取音频流数据，若连续10分钟拉不到流或流数据无人声时，服务会终止识别任务  
• 服务通过回调的方式来提供识别结果，用户需要提供CallbackUrl。回调时机为一小段话(最长15秒)回调一次。  
• 签名方法参考 [公共参数](https://cloud.tencent.com/document/api/1093/35640) 中签名方法v3。  
• 默认单账号限制并发数为20路，如您有提高并发限制的需求，请提[工单](https://console.cloud.tencent.com/workorder/category)进行咨询。

默认接口请求频率限制：20次/秒。

## 2. 输入参数

以下请求参数列表仅列出了接口请求参数和部分公共参数，完整公共参数列表见 [公共请求参数](https://cloud.tencent.com/document/api/1093/35640)。

|参数名称|必选|类型|描述|
|---|---|---|---|
|Action|是|String|[公共参数](https://cloud.tencent.com/document/api/1093/35640)，本接口取值：CreateAsyncRecognitionTask。|
|Version|是|String|[公共参数](https://cloud.tencent.com/document/api/1093/35640)，本接口取值：2019-06-14。|
|Region|否|String|[公共参数](https://cloud.tencent.com/document/api/1093/35640)，此参数为可选参数。|
|EngineType|是|String|引擎模型类型。  <br>• 16k_zh：中文普通话通用；  <br>• 16k_en：英语；  <br>• 16k_id：印度尼西亚语；  <br>• 16k_fil：菲律宾语；  <br>• 16k_th：泰语；  <br>• 16k_pt：葡萄牙语；  <br>• 16k_tr：土耳其语；  <br>• 16k_ar：阿拉伯语；  <br>• 16k_es：西班牙语；  <br>• 16k_hi：印地语；  <br>• 16k_fr：法语；  <br>• 16k_de：德语；  <br>示例值：16k_zh|
|Url|是|String|语音流地址，支持rtmp、rtsp等流媒体协议，以及各类基于http协议的直播流(不支持hls, m3u8)  <br>示例值：https://www.audio.com/audio.wav|
|CallbackUrl|是|String|支持HTTP和HTTPS协议，用于接收识别结果，您需要自行搭建公网可调用的服务。回调格式&内容详见：[语音流异步识别回调说明](https://cloud.tencent.com/document/product/1093/52633)  <br>示例值：https://www.audio.com/callback|
|SignToken|否|String|用于生成回调通知中的签名  <br>示例值：xadjjfshjasklksadlakd|
|FilterDirty|否|Integer|是否过滤脏词（目前支持中文普通话引擎）。0：不过滤脏词；1：过滤脏词；2：将脏词替换为 * 。默认值为 0  <br>示例值：0|
|FilterModal|否|Integer|是否过滤语气词（目前支持中文普通话引擎）。0：不过滤语气词；1：部分过滤；2：严格过滤 。默认值为 0  <br>示例值：0|
|FilterPunc|否|Integer|是否过滤标点符号（目前支持中文普通话引擎）。 0：不过滤，1：过滤句末标点，2：过滤所有标点。默认为0  <br>示例值：0|
|ConvertNumMode|否|Integer|是否进行阿拉伯数字智能转换。0：不转换，直接输出中文数字，1：根据场景智能转换为阿拉伯数字。默认值为1  <br>示例值：1|
|WordInfo|否|Integer|是否显示词级别时间戳。0：不显示；1：显示，不包含标点时间戳，2：显示，包含标点时间戳。默认为0  <br>示例值：0|
|HotwordId|否|String|热词id。用于调用对应的热词表，如果在调用语音识别服务时，不进行单独的热词id设置，自动生效默认热词；如果进行了单独的热词id设置，那么将生效单独设置的热词id。  <br>示例值：sifhsadhuasjh*****djsahdsg|
|AudioData|否|Boolean|回调数据中，是否需要对应音频数据。  <br>示例值：false|

## 3. 输出参数

|参数名称|类型|描述|
|---|---|---|
|Data|[Task](https://cloud.tencent.com/document/api/1093/37824#Task)|请求返回结果，包含本次的任务ID(TaskId)|
|RequestId|String|唯一请求 ID，由服务端生成，每次请求都会返回（若请求因其他原因未能抵达服务端，则该次请求不会获得 RequestId）。定位问题时需要提供该次请求的 RequestId。|

## 4. 示例

### 示例1 语音流异步识别任务创建

创建一个异步识别任务，通过接口返回拿到任务ID

#### 输入示例

```
POST / HTTP/1.1
Host: asr.tencentcloudapi.com
Content-Type: application/json; charset=utf-8
X-TC-Version: 2019-06-14
X-TC-Region: ap-shanghai
X-TC-Action: CreateAsyncRecognitionTask
X-TC-Timestamp: 1599140162
Authorization: TC3-HMAC-SHA256 Credential=************************************************************/2020-09-03/asr/tc3_request, SignedHeaders=content-type;host, Signature=0615f73a69c6b054affd69e4b1cbb68fbe84ae8024a9347df4aa5054933adac8
<公共请求参数>

{
    "EngineType": "16k_zh",
    "Url": "rtmp://test.com/test_stream",
    "CallbackUrl": "http://test.com/callback",
    "SignToken": ""
}
```

#### 输出示例

```json
{
    "Response": {
        "RequestId": "fabc2d63-a1b7-40a0-b4c3-640f78974919",
        "Data": {
            "TaskId": 1000000007
        }
    }
}
```

## 5. 错误码

以下仅列出了接口业务逻辑相关的错误码，其他错误码详见 [公共错误码](https://cloud.tencent.com/document/api/1093/35647#.E5.85.AC.E5.85.B1.E9.94.99.E8.AF.AF.E7.A0.81)。

| 错误码                                 | 描述                        |
| ----------------------------------- | ------------------------- |
| FailedOperation.ServiceIsolate      | 账号因为欠费停止服务，请在腾讯云账户充值。     |
| FailedOperation.UserHasNoAmount     | 资源包耗尽，请购买资源包或开通后付费        |
| FailedOperation.UserHasNoFreeAmount | 资源包耗尽，请开通后付费或者购买资源包       |
| FailedOperation.UserNotRegistered   | 服务未开通，请在腾讯云官网语音识别控制台开通服务。 |
| InternalError.FailAccessDatabase    | 访问数据库失败。                  |
| InvalidParameter                    | 参数错误。                     |
| InvalidParameterValue               | 参数取值错误。                   |
| MissingParameter                    | 缺少参数错误。                   |
| RequestLimitExceeded                | 请求的次数超过了频率限制。             |
| UnknownParameter                    | 未知参数错误。                   |