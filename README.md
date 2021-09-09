# CCD Case Parking Utility

This utility can be used to automatically "park" or "unpark" a given set of cases. 
Parking a case is the process of making a case unavailable to any users or update process that go through CCD APIs, 
while unparking is to make it available again. 

Parking is achieved by setting the state of a case to a dummy value for which no users would have access, 
due to existing CRUD-based access control enforced by CCD APIs.
Unparking is achieved by setting the state back to its original value.

This utility takes in a csv file as an input, containing a list of cases to be actioned. 
Values for each column in each row are verified against the data in the CCD Data Store database prior to any changes being made. 
This includes verification that the case is currently in a parked state.

Any failures in verification will result in no changes being made for any of the dataset. 

## Getting started
### Environment variables
Set the following environment variables as required

| Name | Description | Default |
|------|-------------|---------|
| DATA_STORE_DB_URL | CCD Data Store database URL | `jdbc:postgresql://localhost:5055/ccd_data?stringtype=unspecified` |
| DATA_STORE_DB_USERNAME | Username for database | `ccd` |
| DATA_STORE_DB_PASSWORD | Password for database | `ccd` |
| PARKING_STATE_PREFIX | Prefix for states of parked cases | `PARKED_AT__` | 
| CASE_LIST_FILE | Path to csv | *empty* |
| DRY_RUN | Flag to log only - no changes will be made | `false` |

### Case list file
The case list file must be a csv file, similar to the below example:

```
Jurisdiction,CaseType,Reference,Action
AUTOTEST1,AAT,1111222233334444,PARK
AUTOTEST1,AAT,2222333344445555,UNPARK
AUTOTEST1,AAT,"3333444455556666",PARK
DIVORCE,DIVORCE,'4444555566667777',PARK
```

Case Reference values can optionally be surrounded by single quotes (`'`) or double quotes (`"`).

All columns are mandatory.

Supported `Action` values are `PARK` or `UNPARK`.

### Building
To build the application:

```./gradlew assemble```