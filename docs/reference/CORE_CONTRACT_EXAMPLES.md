# Core Contract Examples

## Example 1: Turn Lounge Light On
Principal: Steve  
Resource: Home Assistant entity `light.lounge`  
Action: CONTROL  
ExecutionRequest: Turn lounge light on  
Permission: Approved  
ExecutionResult: Success

## Example 2: Send Email
Principal: Steve  
Resource: Gmail account  
Action: SEND_EXTERNAL  
ExecutionRequest: Send drafted reply  
Permission: ApprovedWithConfirmation  
ExecutionResult: Success or Cancelled

## Example 3: Plugin Attempts Undeclared Access
Principal: Plugin.DocumentScanner  
Resource: Email inbox  
Action: READ  
Permission: Denied  
ExecutionResult: Not executed
