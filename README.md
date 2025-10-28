[![Project Status: Unsupported – The project has reached a stable, usable state but the author(s) have ceased all work on it. A new maintainer may be desired.](https://www.repostatus.org/badges/latest/unsupported.svg)](https://www.repostatus.org/#unsupported)

## Note: This repository has been archived. There will likely be no further development at this repo, and security vulnerabilities may be unaddressed.

# Bank of Sirius

**Bank of Sirius** is a open source fork of [**Bank of Anthos**](https://github.com/GoogleCloudPlatform/bank-of-anthos). This
project improves upon **Bank of Anthos** by adding additional telemetry, instrumentation, performance tuning, upgraded
libraries, and more. The intention is for it to be a more *productionized* example.

## Project

**Bank of Sirius** is a sample HTTP-based web app that simulates a bank's payment processing network, allowing users to
create artificial bank accounts and complete transactions.

If you’re using this app, please ★Star the repository to show your interest!

## Important Note

This repository is intended to be used with the NGINX Modern Application Architecture (MARA)
project [kic-reference-architectures](https://github.com/nginxinc/kic-reference-architectures). This includes
modifications to the deployment process, as well as the inclusion of OTEL elements that work with the above project.

If you wish to run this outside of the NGINX MARA project you may run into issues, so it is recommended in this case you
either fork this project or the original [Google Bank of Anthos](https://github.com/GoogleCloudPlatform/bank-of-anthos)
project.

### Release Process
This process is intended to be used for releases that are intended to be used with the NGINX Modern Application 
Architecture (MARA) project [kic-reference-architectures](https://github.com/nginxinc/kic-reference-architectures). If 
you are using a forked version of MARA you will want to examine the [GNUMakefile](./GNUmakefile) and adjust your 
repository or other targets as required.

1. Set the version: `make version-set`.
2. Update the maven files: `make update-maven-versions`
3. Update the manifest files: `make update-manifest-image-versions`
4. Build the docker images: `make docker-all-images`
5. Push the resulting images to the container registry: `make release`

## Screenshots

| Sign in                                                                                                        | Home                                                                                                    |
| ----------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------ |
| [![Login](./docs/login.png)](./docs/login.png) | [![User Transactions](./docs/transactions.png)](./docs/transactions.png) |

## Service Architecture

![Architecture Diagram](./docs/architecture.png)

| Service                                          | Language      | Description                                                                                                                                  |
| ------------------------------------------------ | ------------- | -------------------------------------------------------------------------------------------------------------------------------------------- |
| [frontend](./src/frontend)                       | Python        | Exposes an HTTP server to serve the website. Contains login page, signup page, and home page.                                                |
| [ledger-writer](./src/ledgerwriter)              | Java          | Accepts and validates incoming transactions before writing them to the ledger.                                                               |
| [balance-reader](./src/balancereader)            | Java          | Provides efficient readable cache of user balances, as read from `ledger-db`.                                                                |
| [transaction-history](./src/transactionhistory)  | Java          | Provides efficient readable cache of past transactions, as read from `ledger-db`.                                                            |
| [ledger-db](./src/ledger-db)                     | PostgreSQL | Ledger of all transactions. Option to pre-populate with transactions for demo users.                                                         |
| [user-service](./src/userservice)                | Python        | Manages user accounts and authentication. Signs JWTs used for authentication by other services.                                              |
| [contacts](./src/contacts)                       | Python        | Stores list of other accounts associated with a user. Used for drop down in "Send Payment" and "Deposit" forms. |
| [accounts-db](./src/accounts-db)                 | PostgreSQL | Database for user accounts and associated data. Option to pre-populate with demo users.                                                      |
| [loadgenerator](./src/loadgenerator)             | Python/Locust | Continuously sends requests imitating users to the frontend. Periodically creates new accounts and simulates transactions between them.      |

## Troubleshooting

See the [Troubleshooting guide](./docs/troubleshooting.md) for resolving common problems.

## Development

See the [Development guide](./docs/development.md) to learn how to run and develop this app locally.

