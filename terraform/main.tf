terraform {
  // aws 라이브러리 불러옴
  required_providers {
    aws = {
      source = "hashicorp/aws"
    }
  }
}

provider "aws" {
  region = "ap-northeast-2" # 서울 리전
}


// ec2 6대를 추가한다. 6대 는 각각 broker, eureka, gateway, app, auth, dispatcher의 name 태그를 가진다.
// 기본적으로 모두 22번 포트가 in/outbound로 열려 있어야 한다.
// 이에 필요한 security group을 생성한다.
// vpc는 2개로 gateway / etc로 나뉘어야 한다.
// 가용영역은 broker가 scale out될때마다 broker 2개당 1개의 가용영역으로 생성한다.
// vpc는 2개로 gateway / etc로 나뉘어야 한다.
// gateway vpc는 10.0.0.0/24 대역을 사용하고, etc vpc는 10.0.1.0/24 대역을 사용한다.
// 추가로 redis, mysql, kafka용으로 ec2 각각 한대 추가해줘
// vpc, 가용영역은 자의적으로 판단해 설정해줘
resource "aws_instance" "redis" {
  ami           = "ami-03ff09c4b716e6425"
  instance_type = "t2.micro"
  tags = {
    Name = "redis"
  }
  subnet_id = aws_subnet.etc_subnet.id
  vpc_security_group_ids = [aws_security_group.allow_ssh_etc.id]
}

resource "aws_instance" "mysql" {
  ami           = "ami-03ff09c4b716e6425"
  instance_type = "t2.micro"
  tags = {
    Name = "mysql"
  }
  subnet_id = aws_subnet.etc_subnet.id
  vpc_security_group_ids = [aws_security_group.allow_ssh_etc.id]
}

resource "aws_instance" "kafka" {
  ami           = "ami-03ff09c4b716e6425"
  instance_type = "t2.micro"
  tags = {
    Name = "kafka"
  }
  subnet_id = aws_subnet.etc_subnet.id
  vpc_security_group_ids = [aws_security_group.allow_ssh_etc.id]
}


resource "aws_vpc" "gateway" {
  cidr_block = "10.0.0.0/24"
  tags = {
    Name = "gateway-vpc"
  }
}
resource "aws_vpc" "etc" {
  cidr_block = "10.0.1.0/24"
  tags = {
    Name = "etc-vpc"
  }
}

resource "aws_subnet" "gateway_subnet" {
  vpc_id     = aws_vpc.gateway.id
  cidr_block = "10.0.0.0/24"
  availability_zone = "ap-northeast-2a"
  tags = {
    Name = "gateway-subnet"
  }
}

resource "aws_subnet" "etc_subnet" {
  vpc_id     = aws_vpc.etc.id
  cidr_block = "10.0.1.0/24"
  availability_zone = "ap-northeast-2a"
  tags = {
    Name = "etc-subnet"
  }
}



// ami 이미지는 amazon linux 2 여야 한다
resource "aws_security_group" "allow_ssh_gateway" {
  name        = "allow_ssh"
  description = "Allow SSH inbound traffic"
  vpc_id      = aws_vpc.gateway.id

  ingress {
    from_port = 22
    to_port   = 22
    protocol  = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port = 0
    to_port   = 0
    protocol  = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "allow_ssh_etc" {
  name        = "allow_ssh"
  description = "Allow SSH inbound traffic"
  vpc_id      = aws_vpc.etc.id

  ingress {
    from_port = 22
    to_port   = 22
    protocol  = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port = 0
    to_port   = 0
    protocol  = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_instance" "broker" {
  ami           = "ami-03ff09c4b716e6425"
  instance_type = "t2.micro"
  tags = {
    Name = "broker"
  }
  subnet_id = aws_subnet.etc_subnet.id
  vpc_security_group_ids = [aws_security_group.allow_ssh_etc.id]
}

resource "aws_instance" "eureka" {
  ami           = "ami-03ff09c4b716e6425"
  instance_type = "t2.micro"
  tags = {
    Name = "eureka"
  }
  subnet_id = aws_subnet.etc_subnet.id
  vpc_security_group_ids = [aws_security_group.allow_ssh_etc.id]
}

resource "aws_instance" "gateway" {
  ami           = "ami-03ff09c4b716e6425"
  instance_type = "t2.micro"
  tags = {
    Name = "gateway"
  }
  subnet_id = aws_subnet.gateway_subnet.id
  vpc_security_group_ids = [aws_security_group.allow_ssh_gateway.id]
}

resource "aws_instance" "app" {
  ami           = "ami-03ff09c4b716e6425"
  instance_type = "t2.micro"
  tags = {
    Name = "app"
  }
  subnet_id = aws_subnet.etc_subnet.id
  vpc_security_group_ids = [aws_security_group.allow_ssh_etc.id]
}

resource "aws_instance" "auth" {
  ami           = "ami-03ff09c4b716e6425"
  instance_type = "t2.micro"
  tags = {
    Name = "auth"
  }
  subnet_id = aws_subnet.etc_subnet.id
  vpc_security_group_ids = [aws_security_group.allow_ssh_etc.id]
}

resource "aws_instance" "dispatcher" {
  ami           = "ami-03ff09c4b716e6425"
  instance_type = "t2.micro"
  tags = {
    Name = "dispatcher"
  }
  subnet_id = aws_subnet.etc_subnet.id
  vpc_security_group_ids = [aws_security_group.allow_ssh_etc.id]
}