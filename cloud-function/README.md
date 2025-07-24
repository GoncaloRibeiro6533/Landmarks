A classe 'pt.isel.cn.Entrypoint' implementa uma Cloud Function do tipo HTTP que pode ser invocada através de um simples
pedido HTTP. A função é pertence ao projecto 'cn2425-t1-g09', na região 'europe-southwest1', e foi configurada para
permitir chamadas não autenticadas.
O comportamento principal da função consiste em receber um parâmetro de consulta chamado 'name', que representa o nome (
ou parte do nome) de um grupo de instâncias. Com base nesta informação, a função utiliza o serviço Compute Engine
através do cliente 'InstancesClient' e percorre todas as instâncias na zona 'europe-southwest1-b'.
Esta Cloud Function, é utilizada no cliente com objetivo de obter dinamicamente os IPs públicos das instâncias ativas do
grupo de instâncias que executam o servidor.

gcloud functions deploy funcIpLookup
--project=cn2425-t1-g09 --region=europe-southwest1 --allow-unauthenticated
--entry-point=pt.isel.cn.Entrypoint --gen2 --runtime=java21 --trigger-http --source=target/deployment
--service-account=cloud-function@cn2425-t1-g09.iam.gserviceaccount.com --max-instances=3


# Para realizar o deployment da Cloud Function, é necessário executar o seguintes comando no terminal:

```bash
mvn clean package
```

```bash
gcloud functions deploy funcIpLookup
--project=cn2425-t1-g09 --region=europe-southwest1 --allow-unauthenticated
--entry-point=pt.isel.cn.Entrypoint --gen2 --runtime=java21 --trigger-http --source=target/deployment
--service-account=cloud-function@cn2425-t1-g09.iam.gserviceaccount.com --max-instances=3
```