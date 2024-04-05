package teammates.ui.webapi;

import java.util.UUID;

import teammates.common.datatransfer.AccountRequestStatus;
import teammates.common.exception.EntityDoesNotExistException;
import teammates.common.exception.InvalidParametersException;
import teammates.common.util.Const;
import teammates.common.util.EmailWrapper;
import teammates.storage.sqlentity.AccountRequest;
import teammates.ui.output.AccountRequestData;
import teammates.ui.request.AccountRequestUpdateRequest;
import teammates.ui.request.InvalidHttpRequestBodyException;

/**
 * Updates an account request.
 */
public class UpdateAccountRequestAction extends AdminOnlyAction {

    static final String ACCOUNT_REQUEST_NOT_FOUND = "Account request with id = %s not found";

    @Override
    public JsonResult execute() throws InvalidOperationException, InvalidHttpRequestBodyException {
        String id = getNonNullRequestParamValue(Const.ParamsNames.ACCOUNT_REQUEST_ID);
        UUID accountRequestId;

        try {
            accountRequestId = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new InvalidHttpParameterException(e.getMessage(), e);
        }

        AccountRequest accountRequest = sqlLogic.getAccountRequest(accountRequestId);

        if (accountRequest == null) {
            String errorMessage = String.format(ACCOUNT_REQUEST_NOT_FOUND, accountRequestId.toString());
            throw new EntityNotFoundException(errorMessage);
        }

        AccountRequestUpdateRequest accountRequestUpdateRequest =
                getAndValidateRequestBody(AccountRequestUpdateRequest.class);

        if (accountRequestUpdateRequest.getStatus() == AccountRequestStatus.APPROVED
                && (accountRequest.getStatus() == AccountRequestStatus.PENDING
                || accountRequest.getStatus() == AccountRequestStatus.REJECTED)) {
            try {
                // should not need to update other fields for an approval
                accountRequest.setStatus(accountRequestUpdateRequest.getStatus());
                accountRequest = sqlLogic.updateAccountRequest(accountRequest);
                EmailWrapper email = sqlEmailGenerator.generateNewInstructorAccountJoinEmail(
                        accountRequest.getRegistrationUrl(), accountRequest.getEmail(), accountRequest.getName());
                emailSender.sendEmail(email);
            } catch (InvalidParametersException e) {
                throw new InvalidHttpRequestBodyException(e);
            } catch (EntityDoesNotExistException e) {
                throw new EntityNotFoundException(e);
            }
        } else {
            try {
                accountRequest.setName(accountRequestUpdateRequest.getName());
                accountRequest.setEmail(accountRequestUpdateRequest.getEmail());
                accountRequest.setInstitute(accountRequestUpdateRequest.getInstitute());
                accountRequest.setStatus(accountRequest.getStatus());
                accountRequest.setComments(accountRequestUpdateRequest.getComments());
                sqlLogic.updateAccountRequest(accountRequest);
            } catch (InvalidParametersException e) {
                throw new InvalidHttpRequestBodyException(e);
            } catch (EntityDoesNotExistException e) {
                throw new EntityNotFoundException(e);
            }
        }

        return new JsonResult(new AccountRequestData(accountRequest));
    }
}
