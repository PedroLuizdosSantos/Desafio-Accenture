README – Desafio Técnico Accenture

Este projeto foi desenvolvido para o desafio técnico da Accenture.
A ideia foi criar um sistema completo para cadastrar empresas, fornecedores e gerenciar os vínculos entre eles.

Eu não dominava várias das tecnologias usadas aqui (principalmente Spring Boot, JPA/Hibernate, Docker e a parte de integração entre serviços). Então fui aprendendo conforme avançava no próprio projeto. Boa parte do desenvolvimento foi pesquisa, teste e correção até tudo funcionar como esperado.

O objetivo principal foi entregar algo funcional, organizado e simples de rodar.

Tecnologias utilizadas:

-Backend (API REST): Java 17, Spring Boot, Spring Web, Spring Data JPA, Hibernate.
-Frontend: Python 3.11, Flask, Jinja2 e Bootstrap.
-Banco: MySQL.
-Infraestrutura: Docker e Docker Compose.

Como executar:

É necessário ter Docker e Docker Compose instalados.

Para subir tudo:
docker compose up --build

Depois disso:
Frontend: http://localhost:5000

Backend: http://localhost:8081

O MySQL sobe automaticamente no container.

Funcionalidades:

  Empresas:
  
  Cadastro, edição, exclusão e listagem
  
  Validação de CEP
  
  Preenchimento automático de estado
  
  CNPJ único

Fornecedores:

  Cadastro, edição, exclusão e listagem
  
  CPF/CNPJ único
  
  Validação de CEP
  
  Regras para PF (RG obrigatório, data de nascimento obrigatória e bloqueio de menores de idade no Paraná)

Vínculos:

  Associar fornecedor a empresa
  
  Remover vínculo
  
  Evita vínculos duplicados
  
  Interface

Tabelas e formulários simples e diretos

Mensagens de erro e sucesso

Comunicação com o backend via API

Observações sobre o desenvolvimento:

  Tive que aprender várias partes enquanto construía o projeto.
  
  A integração Python ↔ Java foi feita manualmente chamando a API.
  
  Toda a estrutura foi colocada em containers para facilitar a execução.
  
  A validação de CEP usa duas APIs para garantir mais estabilidade.
  
  As regras do desafio foram seguidas conforme o enunciado.
  
  O foco foi ser funcional e fácil de entender.

Estrutura do projeto:

backend-java
static
templates
app.py
docker-compose.yml
Dockerfile.frontend
README.md
requirements.txt

Consideração final

Mesmo não conhecendo tudo no início, fui aprendendo e construindo até chegar numa solução completa e utilizável. O projeto reflete exatamente esse processo: estudo, tentativa e erro, ajuste e entrega.

