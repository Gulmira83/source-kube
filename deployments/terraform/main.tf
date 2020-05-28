module "source_kube" {
  source  = "fuchicorp/chart/helm"
  deployment_name        = "source-kube"
  deployment_environment = "${var.deployment_environment}"
  deployment_endpoint    = "${lookup(var.deployment_endpoint, "${var.deployment_environment}")}"
  deployment_path        = "source-kube"
  template_custom_vars = {
      replicas          = "${var.replicas}"
      deployment_image  = "${var.deployment_image}"
  }
}

variable "deployment_image" {
  default = "docker.fuchicorp.com/source-kube:latest"
}

variable "deployment_environment" {
  default = "dev"
}

variable "replicas" {
  default = "3"
}

variable "deployment_endpoint" {
  type = "map"
  default = {
      dev  = "dev.source.fuchicorp.com"
      qa   = "qa.source.fuchicorp.com"
      prod = "source.fuchicorp.com"
      test = "test.source.fuchicorp.com"
  }
}
