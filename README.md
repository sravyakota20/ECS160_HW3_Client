# Sravya Kota (03/11)
# ECS160-HW3: Client Application

## Overview
This project implements the **Client Application** for ECS160-HW3. It orchestrates the **Moderation Microservice** and **Hashtagging Microservice**, processing social media posts from a dataset and forwarding them through the pipeline.

## Project Structure
This repository consists of:
- **PipelineClient** - Sends social media posts to the moderation and hashtagging services.
- **Persistence Module** - Manages loading and processing posts.
- **JUnit Tests** - Validates the client’s ability to interact with microservices.

