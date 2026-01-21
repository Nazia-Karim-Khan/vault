# Vault Setup Guide

## Install Nginx

```bash
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
helm repo update
helm install nginx-ingress ingress-nginx/ingress-nginx --namespace ingress-nginx --create-namespace
```

## Install CSI Driver

```bash
helm repo add secrets-store-csi-driver https://kubernetes-sigs.github.io/secrets-store-csi-driver/charts
helm pull secrets-store-csi-driver/secrets-store-csi-driver --untar
helm install csi-driver ./secrets-store-csi-driver -n csi --create-namespace -f ./secrets-store-csi-driver/values.yaml
```

## Install and Set Up Vault

```bash
kubectl create namespace vault

helm repo add hashicorp https://helm.releases.hashicorp.com
helm pull hashicorp/vault --version 0.31.0
tar -xvf vault-0.31.0.tgz

helm install vault hashicorp/vault -n vault --create-namespace -f vault/values.yaml

helm upgrade vault hashicorp/vault -n vault --create-namespace -f vault/values.yaml
```

### Initialize Vault

Shell into pod and run:

```bash
vault operator init
```

Then unseal 3 times using the tokens:

```bash
vault operator unseal
vault operator unseal
vault operator unseal
```

Verify status:

```bash
vault status
```

### Access UI

- **Host entry:** `10.10.3.27 vault.example.com`
- **URL:** http://vault.example.com:30080/ui/vault/auth
- **Token:** Root token ()

## Deploy PostgreSQL

### Add Helm Repositories

```bash
# add repo for postgres-operator
helm repo add postgres-operator-charts https://opensource.zalando.com/postgres-operator/charts/postgres-operator

# install the postgres-operator
helm install postgres-operator postgres-operator-charts/postgres-operator

# add repo for postgres-operator-ui
helm repo add postgres-operator-ui-charts https://opensource.zalando.com/postgres-operator/charts/postgres-operator-ui

# install the postgres-operator-ui
helm install postgres-operator-ui postgres-operator-ui-charts/postgres-operator-ui -n postgress --create-namespace
```
### Configure Operator UI

Make `targetNamespace: "*"` in operator-ui deployment and restart


### Create PostgreSQL Instance

```bash
kubectl create -f postgres/manifest.yaml -n postgress

# Port forward to access the UI
kubectl port-forward svc/postgres-operator-ui 8081:80
```


## Configure Vault in UI

### Create Secret Engine

In Vault UI:

1. Create secret engine in vault from UI
   - Choose Infra: Database
   - Set a path (e.g., `database`)

### Create Database Connection

1. Create database connection:
   - Select plugin (PostgreSQL)
   - Connection URI: `postgresql://{{username}}:{{password}}@acid-minimal-cluster.postgress.svc.cluster.local:5432/foo`
   - Provide username and password of DB user
   - Set `password_authentication` to `scram-sha-256`
   - Select "Enable without rotating"

### Create Role

1. Give a name (e.g., `my-role`)
2. Select connection name
3. Select type: `dynamic`
4. Add Create statement:

```sql
CREATE ROLE "{{name}}" WITH LOGIN PASSWORD '{{password}}' VALID UNTIL '{{expiration}}';
GRANT ALL PRIVILEGES ON DATABASE foo TO "{{name}}";
```

### Create Kubernetes Auth

1. Create auth choosing Kubernetes from Access
2. Set kubernetes_host to `https://10.10.3.20:6443` in configuration

### Create Access Policy

1. Create policy: `internal-app`
2. Create access policy with the following content:

```
path "database/creds/my-role" {
  capabilities = ["read"]
}
```

### Bind Role with Access Policy

1. Create role named `database`
2. Add bound service account names (e.g., `webapp-sa` - the service account of the application to be deployment)
3. Bound service account namespaces (e.g., `spring` - application deployment namespace)
4. Choose policy: `internal-app` 



## Create SecretProviderClass

```bash
kubectl apply -f db-secret-provider.yaml -n spring
```

## Deploy Application

```bash
kubectl apply -f deployment-service.yaml -n spring
```

### Verify Deployment

Shell into pod and verify:

```bash
cat /mnt/secrets-store/db-password
```

## Deploy Application with Vault Injector

Using CSI won't refresh the secrets when they expire. To refresh the secrets automatically, we'll be using Vault Injector.

### Apply Deployment

Apply the `deployment-service.yaml` file. This will create a file in the pod at `/vault/secrets` named `database.properties`:

```bash
kubectl apply -f deployment-service.yaml -n spring
```

### Verify Secrets

Shell into the pod and view the generated secrets:

```bash
cat /vault/secrets/database.properties
```

Expected output:

```properties
spring.datasource.url=jdbc:postgresql://acid-minimal-cluster2.postgress.svc.cluster.local:5432/foo2
spring.datasource.username=<username>
spring.datasource.password=<password>
```

### Configure TTL

Set the role of secret engine database with the following TTL settings:

| Property | Value |
|----------|-------|
| **Generated credentials's Time-to-Live (TTL)** | 2 minutes |
| **Generated credentials's maximum Time-to-Live (Max TTL)** | 3 minutes |

You can observe the refreshed secrets at pod logs after 3 minutes


## References

- [Postgres Operator Quickstart](https://github.com/zalando/postgres-operator/blob/master/docs/quickstart.md#helm-chart)
- [Vault Kubernetes Secret Store Driver](https://developer.hashicorp.com/vault/tutorials/kubernetes-introduction/kubernetes-secret-store-driver)
- [Spring Reload Secrets](https://developer.hashicorp.com/vault/tutorials/app-integration/spring-reload-secrets#reload-dynamic-secrets)
- [Vault PostgreSQL Documentation](https://developer.hashicorp.com/vault/docs/secrets/databases/postgresqls)
- [Refresh Credentials Dynamically](https://www.hashicorp.com/en/blog/refresh-secrets-for-kubernetes-applications-with-vault-agent#update-secrets-by-application-restart)
- [Vault Injector](https://developer.hashicorp.com/vault/docs/deploy/kubernetes/injector)
- [Vault Injector Annotations](https://developer.hashicorp.com/vault/docs/deploy/kubernetes/injector/annotations)
- [Demo App](https://github.com/hashicorp-dev-advocates/workshop-vault-for-developers/blob/main/payments-app/java/src/main/java/com/hashicorpdevadvocates/paymentsapp/VaultRefresher.java)
