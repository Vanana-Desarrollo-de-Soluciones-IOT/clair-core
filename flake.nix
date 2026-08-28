{
  description = "Development environment for clair-core";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
  };

  outputs = { nixpkgs, ... }:
    let
      supportedSystems = [
        "x86_64-linux"
        "aarch64-linux"
        "x86_64-darwin"
        "aarch64-darwin"
      ];

      forEachSystem = nixpkgs.lib.genAttrs supportedSystems;
    in
    {
      devShells = forEachSystem (system:
        let
          pkgs = import nixpkgs { inherit system; };
        in
        {
          default = pkgs.mkShell {
            packages = with pkgs; [
              jdk25
              maven
              stripe-cli
              curl
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
          };
        });
    };
}
