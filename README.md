# Plugin Maven: encrypt-env-properties

**Versión:** 1.0.0  
**Autor:** Jose Antonio Diaz  
**Descripción:** Plugin Maven personalizado que cifra archivos `.properties` utilizando Jasypt con AES 256. Conserva la estructura de carpetas y permite excluir claves específicas.

---

## Objetivo

Permitir que los proyectos Java cifren sus archivos `.properties` sensibles durante la compilación o como paso previo al empaquetado, sin modificar su estructura de carpetas.

---

## Estructura esperada

### Carpeta de entrada (fuente sin cifrar):

```
src/main/properties-noencript/
├── encrypt-env.properties                <-- archivo de exclusiones
└── properties/
    ├── sistema1/
    │   └── archivo1.properties
    └── sistema2/
        └── subcarpeta/
            └── archivo2.properties
```

### Carpeta de salida (generada automáticamente):

```
properties/
├── sistema1/
│   └── archivo1.properties   <-- valores cifrados
└── sistema2/
    └── subcarpeta/
        └── archivo2.properties
```

---

## Exclusiones de cifrado

Se definen en el archivo:

```
src/main/properties-noencript/encrypt-env.properties
```

### Ejemplo:

```properties
db.username=
server.port=
logging.level.root=
```

> Cada clave definida aquí **no será cifrada**.

---

## Configuración del plugin en `pom.xml`

En el proyecto consumidor:

```xml
<plugin>
    <groupId>com.dizan.plugins</groupId>
    <artifactId>encrypt-env-properties</artifactId>
    <version>1.0.0</version>
    <executions>
        <execution>
            <id>encrypt-properties</id>
            <goals>
                <goal>encrypt-env-properties</goal>
            </goals>
            <phase>generate-resources</phase>
        </execution>
    </executions>
    <configuration>
        <secretKey>${env.CLVMST}</secretKey>
    </configuration>
</plugin>
```

---

## ¿Qué es `${env.CLVMST}`?

Es una **variable de entorno del sistema operativo** que contiene la clave secreta para encriptar.

### Cómo definirla antes de compilar:

#### Windows CMD:
```cmd
set CLVMST=mi_clave_secreta
```

#### PowerShell:
```powershell
$env:CLVMST = "mi_clave_secreta"
```

#### Linux / macOS:
```bash
export CLVMST=mi_clave_secreta
```

---

## Ejecución del plugin

### Durante el build:
```bash
mvn clean install
```

### Manualmente:
```bash
mvn com.gs.dirsapfi.plugins:encrypt-env-properties:1.0.0:encrypt-env-properties -DsecretKey=mi_clave_secreta
```

---

## Requisitos técnicos

| Requisito       | Versión mínima recomendada |
|-----------------|----------------------------|
| Java            | 17                         |
| Maven           | 3.6+                       |
| Jasypt          | 1.9.3                      |

---

## Buenas prácticas

- No versionar los archivos generados en `properties/` si contienen secretos cifrados.
- Controlar la clave `CLVMST` mediante scripts seguros o mecanismos de CI/CD.
- Si deseas cifrar desde un `keystore`, considera extender el plugin con lectura JCE.
