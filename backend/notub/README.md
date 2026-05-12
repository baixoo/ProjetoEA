# NoTUB Backend

## Stack de Dependências

O projeto depende dos seguintes pacotes principais configurados no `pom.xml`:

- **Spring Boot Web (`spring-boot-starter-webmvc`)**: Fornece o servidor HTTP embbedded Tomcat.
- **Spring Boot Data JPA (`spring-boot-starter-data-jpa`)**: Hibernate e JPA.
- **Spring Boot Security (`spring-boot-starter-security`)**: Inicializa as funcionalidades de segurança enterprise, interceptando os endpoints web e executando validações de credenciais.
- **Driver PostgreSQL (`postgresql`)**: Contém o adaptador JDBC.

## Desenvolvimento Local & Configuração de Segurança

### Contornar temporariamente autenticação

Ao ativar o módulo `spring-boot-starter-security`, é automaticamente aplicado um conjunto de regras restritas que exigem autenticação em toda a API (redirecionando qualquer pedido para uma página `/login` gerada automaticamente).

Para permitir o desenvolvimento e testar os controladores durante fases iniciais, foi criada uma classe personalizada **`SecurityConfig`** em `pt.notub.config.SecurityConfig` para dar override das configs default.

### Exceções atualmente ativas

1. **API sem restrições**: O matcher de caminhos define `"/api/**"` com `.permitAll()`, removendo qualquer necessidade de autenticação para endpoints dentro de `/api/`, permitindo acesso direto durante testes iniciais.

2. **CSRF desativado**: A proteção contra Cross-Site Request Forgery (CSRF) foi desativada. Isto permite que pedidos `POST`, `PUT` e `DELETE` feitos a partir de ferramentas externas (como Postman) sejam usadas sem os tokens CSRF.

> **Aviso:** 
> Depois de configurado auth remover o override`.
