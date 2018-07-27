# Drifting Souls

This is the main repository for the browser game [Drifting Souls](https://ds2.drifting-souls.net/).

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes.

### Prerequisites

Required:

* [Java SDK Version 10](http://www.oracle.com/technetwork/java/javase/downloads/jdk10-downloads-4416644.html) or above
* [Maven](https://maven.apache.org/) - Dependency Management
* any Mysql-Server, for example the one bundled with [XAMPP](https://www.apachefriends.org/de/index.html)
* Tomcat, for example the one bundled with XAMPP

Optional:

* any Java IDE, for example [Eclipse](https://www.eclipse.org/) 

### Installing

1. Go to the DS-root directory, open the console and type

```
mvn package
```

2. Start up your MySQL-Server.

Sometimes the timezone of the MySQL-Server won't be recognized. In this case you have to set it manually:

```
SET GLOBAL time_zone = '+2:00';
```

You also have to import the standard-database-layout of DS. Currently there is no easy way to do it. We're working on it.

3. Start up Tomcat.

4. Use the manager-tool of Tomcat to deploy the DS-webarchive. It's located in the folder DS-root\game\target

5. You should be able to use DS on your local machine.


## Development


### HTML
HTML-files are located at

```
DS-root\game\src\main\templates
```

They get compiled to .java-files, which get stored at

```
DS-root\game\target\generated-sources\templates\net\driftingsouls\ds2\server\framework\templates
```

Any changes to them are lost after the next run of the compiler. Use them for debugging only.

### Controllers
The behavior of sites is mainly controlled by controllers.

They are located at

```
DS-root\game\src\main\java\net\driftingsouls\ds2\server\modules
```

The engine will automatically detect new controllers and new actions of controllers.

Every controller must have a name that is followed by Controller and every actions name must be followed by Action. 