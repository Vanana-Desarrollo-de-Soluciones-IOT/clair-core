{ pkgs ? import <nixpkgs> { } }:

pkgs.mkShell {
  packages = with pkgs; [
    jdk25
    maven
    stripe-cli
  ];

  # Keep Maven/Java aligned with pom.xml (<java.version>25</java.version>).
  shellHook = ''
    export JAVA_HOME="${pkgs.jdk25}"
  '';
}
