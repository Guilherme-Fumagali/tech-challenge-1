resource "aws_sqs_queue" "os_eventos_dlq" {
  name                      = "${local.fila_entrada}-dlq"
  message_retention_seconds = 1209600
  sqs_managed_sse_enabled   = true
}

resource "aws_sqs_queue" "os_eventos" {
  name                       = local.fila_entrada
  visibility_timeout_seconds = 30
  message_retention_seconds  = 345600
  receive_wait_time_seconds  = 20
  sqs_managed_sse_enabled    = true

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.os_eventos_dlq.arn
    maxReceiveCount     = 5
  })
}

resource "aws_sqs_queue_redrive_allow_policy" "os_eventos_dlq" {
  queue_url = aws_sqs_queue.os_eventos_dlq.id

  redrive_allow_policy = jsonencode({
    redrivePermission = "byQueue"
    sourceQueueArns   = [aws_sqs_queue.os_eventos.arn]
  })
}
