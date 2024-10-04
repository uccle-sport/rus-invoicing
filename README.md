This project is used to create UBL invoices from the data of an export of iClub.

To run the project, use the following command:
```bash
./gradlew run --args="./export ./cotis.csv ./coda.COD"```
```

This will generate one UBL invoice for each row in the cotis.csv file for which a payment can be found in the CODA.
