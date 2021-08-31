# CCD Case Parking Utility

This utility can be used to automatically park or unpark a given set of cases.

*Note: Currently only unparking is supported.*

Values for each column in each row are verified against the data in the database prior to any changes being made. 
This includes verification that the case is currently in a parked state.

Any failures in verification will result in no changes being made for any of the dataset. 

## Getting started
### Environment variables
Set the following environment variables as required

| Name | Default | Description | Default |
|------|---------|-------------|---------|
| DATA_STORE_DB_URL | CCD Data Store database URL |  jdbc:postgresql://localhost:5055/ccd_data?stringtype=unspecified |
| DATA_STORE_DB_USERNAME | Username for database | ccd |
| DATA_STORE_DB_PASSWORD | Password for database | ccd |
| CASE_LIST_FILE | Path to csv | *empty* |

### Case list file
The application takes in a csv file as an input. This file should take the format:

```
Jurisdiction,CaseType,Case
AUTOTEST1,AAT,1111222233334444
AUTOTEST1,AAT,2222333344445555
AUTOTEST1,AAT,3333444455556666
DIVORCE,DIVORCE,4444555566667777
```

The case list file will in future be extended to support the option to park or unpark each provided case. 
Currently all provided cases will be unparked.