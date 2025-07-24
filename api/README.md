O contrato gRPC, definido no ficheiro image.proto, especifica as funções e mensagens necessárias para as operações funcionais do sistema CN2425TF. Utiliza as dependências grpc-stub, grpc-protobuf e grpc-netty-shaded.

O contrato define quatro operações no serviço Image:
1.	processImage: Client streaming, recebe um fluxo de mensagens Chunk (nome da imagem e conteúdo em bytes) e retorna uma mensagem Identifier com o ID gerado pelo servidor que identifica o pedido.
2.	correspondingImage: Unária, recebe uma mensagem Identifier e retorna ExistingMonuments, contendo uma lista de Monument (nome, localização e confiança).
3.	getMapImage: Unária, recebe uma mensagem Identifier e retorna MapImage, com a imagem estática do Google Maps em bytes.
4.	getMonumentAboveConfidence: Unária, recebe uma mensagem Confidence (valor de confiança) e retorna PhotosList, com uma lista de PhotoName (nome, localização e confiança de fotos acima do limiar).
