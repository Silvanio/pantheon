-- Composite indexes for query shapes confirmed to already exist in the codebase (dashboard
-- summaries, list filters, and access-check lookups) but not covered by any existing single-
-- column index. See openspec/changes/archive/*-add-performance-indexes*/design.md (or the ORM
-- audit that produced this migration) for the query-by-query justification behind each one.

-- PurchaseRequestController.list()'s status filter and SiteSummaryService.purchaseRequestsSummary()'s
-- countByConstructionSiteIdAndStatusAndSubmittedAtIsNotNull.
CREATE INDEX IF NOT EXISTS idx_purchase_request_site_status
    ON purchase_request (construction_site_id, status);

-- SiteSummaryService.orcamentosSummary()'s countByConstructionSiteIdAndStatus.
CREATE INDEX IF NOT EXISTS idx_orcamento_site_status
    ON orcamento (construction_site_id, status);

-- PurchaseRequestApprovalRepository's per-row lookup in PurchaseRequestService.list() (for
-- VIEW_AND_APPROVE callers) and again on every approveStep/rejectStep/getComparison call.
CREATE INDEX IF NOT EXISTS idx_purchase_request_approval_pr_cycle_status_step
    ON purchase_request_approval (purchase_request_id, cycle_number, status, step_order);

-- SiteAccessService.resolve()'s exact-match membership lookup, hit on essentially every
-- authenticated site-scoped request.
CREATE INDEX IF NOT EXISTS idx_site_membership_site_user
    ON site_membership (construction_site_id, user_id);

-- PurchaseRequestService.notifyStepPending()'s findByConstructionSiteIdAndFunction.
CREATE INDEX IF NOT EXISTS idx_site_membership_site_function
    ON site_membership (construction_site_id, function);

-- TaskCardService.getBoard()'s findByConstructionSiteIdOrderBySortOrderAsc and
-- GlobalTaskBoardService.build()'s findByConstructionSiteIdInOrderBySortOrderAsc.
CREATE INDEX IF NOT EXISTS idx_task_card_site_sort_order
    ON task_card (construction_site_id, sort_order);

-- SiteSummaryService.tasksSummary()'s findTop5ByConstructionSiteIdOrderByCreatedAtDesc — a
-- different sort column than the board view above, so it needs its own composite.
CREATE INDEX IF NOT EXISTS idx_task_card_site_created_at
    ON task_card (construction_site_id, created_at);

-- SiteSummaryService.projectsSummary()'s findTop5ByConstructionSiteIdOrderByCreatedAtDesc on
-- SiteDocumentProjectRepository.
CREATE INDEX IF NOT EXISTS idx_site_document_project_site_created_at
    ON site_document_project (construction_site_id, created_at);
