# Keycloak handles identity only

The existing Keycloak server authenticates people (sign-in, passwords, social login) and nothing more; Shops, Members, invitations and the Owner/Designer roles are stored and managed in Open Cabinet's own PostgreSQL, keyed by the Keycloak subject. We chose this over modelling Shops as Keycloak groups and roles because Shops are product data: keeping them in Keycloak would make every new Shop and invitation an admin-API call and tie the domain to Keycloak's realm configuration.
