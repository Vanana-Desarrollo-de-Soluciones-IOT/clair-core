{ pkgs ? import <nixpkgs> {} }:

pkgs.mkShell {
  packages = with pkgs; [
    jdk25
    maven
    stripe-cli
  ];

  shellHook = ''
    export JAVA_HOME="${pkgs.jdk25}"
    echo ""
    echo "Environment ready (JDK 25 + Maven)."
    echo "Run app:      mvn spring-boot:run"
    echo "Run tests:    mvn test"
    echo "Build jar:    mvn clean package"
    echo "Clean only:   mvn clean"
    echo ""
  '';
}
