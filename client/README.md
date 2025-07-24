Módulo responsável por enviar imagens e pedir os resultados ao sistema. O cliente lê o ficheiro de imagem local em
chunks (fragmentos) e usa o stub gRPC para enviar esses chunks ao servidor através de uma chamada de streaming. Por
exemplo, o cliente abre o ficheiro com try-with-resources e faz leitura em blocos de 64KB, enviando cada chunk através
do StreamObserver fornecido pelo stub não-bloqueante. Cada chunk inclui o nome original do ficheiro e um segmento de
bytes da imagem. O cliente recebe do servidor um identificador único (UUID em formato texto) quando o upload termina, e
pode depois pedir os resultados (lista de monumentos detetados, mapa estático, etc.) usando outras chamadas gRPC.

Ao inicializar observa-se também que o cliente consulta a Lookup Function via HTTP e obtém dinamicamente o IP dos
servidores disponíveis no instance group. O endereço IP é escolhido aleatoriamente e em caso da ligação falhar, a
aplicação tenta outro IP ou atualiza a lista através de um novo lookup até estabelecer a conexão. A comunicação com o
servidor gRPC é realizada através de um ManagedChannel, usando stubs gerados a partir do contrato protobuf, stubs
síncronos ImageBlockingStub e assíncronos ImageStub.

MENU:
1 - Submit a monument photo
2 - List all monuments of a photo
3 - Show static map of a geolocation
4 - Get photo names of monuments above a certain confidence
99 - Exit


### Para executar o cliente, é necessário definir as seguintes variáveis de ambiente:

```bash
mvn clean package
```

```bash
java -jar target/client-1.0-SNAPSHOT-jar-with-dependencies.jar
```
