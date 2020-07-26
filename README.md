# Electric Charging Station Operations (ECSO) Server
This app provides infrastructure to operate a chain of electric vehicle (EV) charging stations.
To keep things as simple as possible this app comes with a hardcoded setup of EV charging stations which consists
of 3 charging stations per se each of which has 3 terminals (CT). When EV starts charging session, the terminal becomes 
unavailable for other vehicles to connect to. Once charging session is over, the terminal becomes available again.

## Test
To run unit tests type `sbt test`

## Build
In order to build the app from sources you'll need [SBT](https://www.scala-sbt.org/) of version 1.2.1 or greater
To start the build job - run `./build.sh`

## Run
You can run the as a regular Java application or using Docker.
If the build job ran successfully, the docker image `alekslitvinenk/ecso` should be available locally.
To run the app type or copy'n'paste `docker run -p 8080:8080 alekslitvinenk/ecso` in terminal.

## Exposed endpoint
There two sets of endpoints:
- Charging stations' endpoints
- Backoffice endpoint

### Charging stations' endpoints
Charging stations' endpoints are designated for EV and CT interoperation i.e starting/finishing charging sessions.
Basically, there are 2 endpoint:
- One to start charging session
- Another one to finish charging session

The table below offers brief overview of these endpoints.

| Endpoint  | Authentication | Description | Method | Input | Output |
| :-------: | :-------------:| :---------: | :----: | :---: | :----: |
| `charging/start` | No | Starts charging session at given charging terminal | POST | See [StartSession](#Start/Finish Session format) | See [StartSessionResponse](#StartSessionResponse format) |
| `charging/finish` | No | Finishes charging session at given charging terminal and returns session stats | POST | See [FinishSession](#Start/Finish Session format) | See [SettledSession](#SettledSession format) |

### Backoffice endpoints
Backoffice endpoints provide 4 endpoint:
- For a driver to get an overview of his/her charging sessions
- For a bookkeeper to keep track of the all sessions
- For a supervisor to submit new tariff plans
- For a charging terminal to bill the finished session

The following table shows input parameters and output date for said endpoints.

| Endpoint  | Authentication | Description | Method | Input | Output |
| :-------: | :-------------:| :---------: | :----: | :---: | :----: |
| `backoffice/driver` | No | Returns all finished sessions for a given driver | GET | Mandatory request parameter `driverId` | CSV file containing driver's finished sessions |
| `backoffice/bookkeeper` | No | Returns all finished sessions within the system | GET | - | CSV file containing all finished sessions |
| `backoffice/supervisor/tariff` | Basic HTTP | Adds new tariff plan that starts in the future | POST | See [TariffPlan](#TariffPlan format) | See [SubmitTariffPlanResponse format](#SubmitTariffPlanResponse format) |
| `backoffice/bill-session` | No | Process given session and calculates total bill to invoice the driver | POST | See [BillSession](#BillSession format) | See [SettledSession](#SettledSession format)

### JSON Formats
On the top-level application works with the following json messages

#### Start/Finish Session format
```json
{
	"driverId": 61, // driver Id
	"stationId": 3, // station Id
	"terminalId": 1 // terminal Id
}
```

#### StartSessionResponse format
```json
{
    "sessionTicket": "dd8bfcd1-7748-4865-bf69-c78a91154844",
    "status": {
        "code": 0,
        "message": "Ok"
    }
}
```

#### TariffPlan format
```json
{
    "activationTime": "2020-07-09T19:04:08Z", // time when the given tariff plan comes into force
    "energyConsumptionFee": 42.00, // energy tariff (kWh)
    "parkingFee": 20.00, // optional parking fee ($/h), you can safely omit this field if you don't want to charge you customers parking fee
    "serviceFee": 0.5 // service fee (% from energy total plus parking total)
}
```

#### SubmitTariffPlanResponse format
```json
{
    "status": {
        "code": 0,
        "message": "Ok"
    }
}
```

#### BillSession format
```json
{
    "finishedChargingSession": {
       "consumedEnergy": 0.013888888888888890,
       "driverId": 61,
       "finishTime": "2020-07-09T15:47:54Z",
       "sessionTicket": "dd8bfcd1-7748-4865-bf69-c78a91154844",
       "startTime": "2020-07-09T15:47:49Z"
   },
    "requestId": 3 
}
```

#### SettledSession format
```json
{
    "sessionSettlement": {
        "session": {
            "consumedEnergy": 0.013888888888888890,
            "driverId": 61,
            "finishTime": "2020-07-09T15:47:54Z",
            "sessionTicket": "dd8bfcd1-7748-4865-bf69-c78a91154844",
            "startTime": "2020-07-09T15:47:49Z"
        },
        "tariff": {
            "activationTime": "2020-07-09T15:47:40Z",
            "energyConsumptionFee": 10,
            "serviceFee": 0.15
        },
        "totals": {
            "energyTotal": 0.14,
            "parkingTotal": 0.00,
            "serviceTotal": 0.02,
            "totalAll": 0.16
        }
    },
    "status": {
        "code": 0,
        "message": "Ok"
    }
}
```