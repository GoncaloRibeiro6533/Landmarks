Esta aplicação, responsável pelo processamento de imagens, atua como um consumidor da subscrição 'subscription-1',
previamente criada através da interface do Google Cloud para escutar o tópico Pub/Sub. Sempre que uma nova mensagem é
publicada nesse tópico, o handler definido na construção do Subscriber é acionado para processar o conteúdo da mensagem.

A mensagem recebida deve conter três atributos essenciais: o 'id' do pedido, o nome do 'bucket' e o nome do 'blob', onde
está armazenada a imagem a ser analisada. Com essas informações, a aplicação acede ao serviço Google Cloud Storage para
obter a imagem correspondente, usando o nome do bucket e do blob.

Depois de obter os bytes da imagem, a aplicação invoca a API de deteção de monumentos (VisionUtil.detectLandmarks) que
identifica marcos geográficos ou monumentos presentes na imagem. O resultado desta análise é encapsulado num objeto
Result, contendo a lista de monumentos identificados, e é posteriormente armazenado na coleção 'monuments_results' do
Firestore, associando os dados ao ID do pedido original.

Uma vez concluído o processamento e armazenado o resultado, a aplicação envia um acknowledgment (ack) ao Pub/Sub,
sinalizando que a mensagem foi tratada com sucesso. Caso ocorra algum erro durante o processo, um negative
acknowledgment (nack) é enviado, permitindo que a mensagem seja reprocessada posteriormente.


### Para executar o worker, é necessário definir as seguintes variáveis de ambiente:

- GOOGLE_APPLICATION_CREDENTIALS=`<path>`
  - Chave de uma conta de serviço com Roles: 
    - Cloud Storage Admin
    - Pub/Sub Admin
    - Firestore Admin
- PROJECT_ID=`<project_id>`
  - ID do projeto Google Cloud.

### Para executar o worker, use o seguinte comando:

```bash
mvn clean package
```

```bash
java -jar target/worker-1.0-SNAPSHOT-jar-with-dependencies.jar
```
  
