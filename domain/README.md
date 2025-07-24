Este módulo auxiliar define classes que representam entidades para suportar operações funcionais. É responsável pela
definição de classes como MonumentDomain, Result, LocationDomain, entre outras. Estas servem para encapsular os dados
trocados entre os diferentes componentes do sistema incluindo funcionalidades como envio de blocos de imagens, interação
com o Firestore, gestão de endereços IP, representação de localizações geográficas, modelagem de monumentos com nome,
localização e confiança e formatação de resultados.
Adicionalmente, é importante de destacar que, a interação com o Firestore realizada através da classe utilitária
DatabaseUtil, que centraliza o processo de ligação à base de dados. Começa por obter as credenciais da Google Cloud
Platform, utilizando GoogleCredentials.getApplicationDefault().
Constrói, depois, uma instância de FirestoreOptions com as configurações do grupo, incluindo o ID da base de dados e as
credenciais obtidas. Finalmente, é retornada uma instância de Firestore usada para aceder às coleções, sendo o nome da
coleção tipicamente fornecido como argumento.  
