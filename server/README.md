RPC definidos no proto através de uma classe de serviço (Service ou ImageServiceImpl) que extende
ImageGrpc.ImageImplBase. Este componente recebe os streams de imagens do cliente e coordena o fluxo do trabalho
principal: guarda as imagens na cloud (Google Cloud Storage), publica mensagens de notificação (Google Pub/Sub) para
processamento assíncrono pelo worker, e responde a consultas dos resultados.

O servidor está desenhado para ser stateless no que toca a informações de sessão – isto é, cada pedido carrega consigo
os dados necessários (por ex., ID da imagem) – permitindo escalabilidade horizontal com múltiplas réplicas. Além do RPC
de upload (processImage), o servidor implementa RPCs para obter o mapa de localização (getMapImage), listar monumentos
acima de um certo nível de confiança (getMonumentAboveConfidence) e obter monumentos detetados numa foto pelo seu ID (
correspondingImage). Internamente, o servidor utiliza dependências geridas pela GCP (Firestore, Cloud Storage, Pub/Sub),
mantendo conexões persistentes ou lazy conforme apropriado, mas evitando partilhar estado mutável entre threads.



### Para executar o servidor, é necessário definir as variáveis de ambiente com as credenciais do Google Cloud e a chave da API do Google Maps.

- GOOGLE_APPLICATION_CREDENTIALS=`<path>`
  - Chave de uma conta de serviço com Roles: 
    - Cloud Storage Admin
    - Pub/Sub Admin
    - Firestore Admin
- GOOGLE_MAPS_API_KEY=`<key`


### Para executar o servidor, use o seguinte comando:

```bash
mvn clean package
```

```bash
java -jar target/server-1.0-SNAPSHOT-jar-with-dependencies.jar
```
