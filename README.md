# Desafio Técnico Accenture

Este projeto foi desenvolvido para o desafio técnico da Accenture.  
O objetivo é implementar um sistema para cadastro de empresas, fornecedores e gerenciamento de vínculos entre eles.

Durante o desenvolvimento, muitas tecnologias foram aprendidas na prática, como Spring Boot, JPA/Hibernate, Docker, integração entre serviços e comunicação entre Python e Java.  
O foco foi entregar uma solução funcional, simples de executar e alinhada aos requisitos.

## Tecnologias utilizadas

### Backend
- Java 17
- Spring Boot
- Spring Web
- Spring Data JPA
- Hibernate

### Frontend
- Python 3.11
- Flask
- Jinja2
- Bootstrap

### Banco de dados
- MySQL

### Infraestrutura
- Docker
- Docker Compose

## Como executar

É necessário ter Docker e Docker Compose instalados.

Para iniciar todos os serviços:

```
docker compose up --build
```

Após a execução:

- Frontend disponível em: http://localhost:5000  
- Backend disponível em: http://localhost:8081  
- MySQL é iniciado automaticamente no container

## Funcionalidades

### Empresas
- Cadastro
- Edição
- Exclusão
- Listagem
- Validação de CEP
- Preenchimento automático de estado (UF)
- CNPJ único

### Fornecedores
- Cadastro
- Edição
- Exclusão
- Listagem
- CPF/CNPJ único
- Validação de CEP
- Regras para pessoa física:
  - RG obrigatório
  - Data de nascimento obrigatória
  - Bloqueio de cadastro para menores de idade quando vinculados a empresas do Paraná

### Vínculos
- Associar fornecedor a empresa
- Evitar vínculos duplicados
- Remover vínculos
- Listar fornecedores por empresa

## Interface do sistema

- Formulários simples e diretos
- Tabelas responsivas
- Mensagens de erro e sucesso
- Comunicação com o backend via API REST

## Estrutura do projeto

```
/
├── backend-java/
│   ├── models/
│   ├── controllers/
│   ├── repositories/
│   └── ...
├── app.py
├── templates/
├── static/
├── docker-compose.yml
├── Dockerfile.frontend
├── requirements.txt
└── README.md
```

## Observações sobre o desenvolvimento

- Muitas tecnologias foram aprendidas ao longo do projeto.
- A comunicação entre o backend Java e o frontend em Python foi feita via chamadas REST.
- Toda a aplicação foi containerizada para facilitar a execução.
- A validação de CEP utiliza duas APIs (cep.la e ViaCEP) para maior estabilidade.
- Todas as regras propostas no desafio foram implementadas.

## Consideração final

O projeto representa um processo de aprendizado contínuo, tentativa e erro, ajustes e entrega final.  
A solução está funcional, organizada e pronta para ser executada através de containers.
