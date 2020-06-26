module "source_kube" {
  source  = "fuchicorp/chart/helm"
  deployment_name        = "source-kube"
  deployment_environment = "${var.deployment_environment}"
  deployment_endpoint    = "${lookup(var.deployment_endpoint, "${var.deployment_environment}")}.${var.google_domain_name}"
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
      dev  = "dev.source"
      qa   = "qa.source"
      prod = "source"
      test = "test.source"
      stage  = "stage.source"
  }
}
variable "google_domain_name" {
 default = "fuchicorp.com"

}