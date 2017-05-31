        package fk.retail.ip.requirement.internal.enums;

        import java.util.Arrays;
        import java.util.List;

        /**
         * Created by nidhigupta.m on 14/03/17.
         */
        public enum RequirementExcelHeaders {
            REQUIREMENT_ID("Requirement Id"),
            WAREHOUSE("Warehouse"),
            FSN("FSN"),
            VERTICAL("Vertical"),
            CATEGORY("Category"),
            SUPER_CATEGORY("Super Category"),
            TITLE("Title"),
            BRAND("Brand"),
            PV_BAND("PV Band"),
            SALES_BAND("Sales Band"),
            FLIPKART_SELLING_PRICE("Flipkart Selling Price"),
            SALES_BUCKET_0("Sales bucket-0"),
            SALES_BUCKET_1("Sales bucket-1"),
            SALES_BUCKET_2("Sales bucket-2"),
            SALES_BUCKET_3("Sales bucket-3"),
            SALES_BUCKET_4("Sales bucket-4"),
            SALES_BUCKET_5("Sales bucket-5"),
            SALES_BUCKET_6("Sales bucket-6"),
            SALES_BUCKET_7("Sales bucket-7"),
            INVENTORY("Inventory"),
            INTRANSIT("Intransit"),
            FORECAST("Forecast"),
            QOH("QOH"),
            PROCUREMENT_TYPE("Procurement Type"),
            CURRENCY("Currency"),
            PURCHASE_PRICE("Purchase Price"),
            MRP("MRP"),
            SLA("SLA"),
            TOTAL_VALUE("Total Value"),
            SUPPLIER("Supplier"),
            QUANTITY("Quantity"),
            IPC_PROPOSED_QUANTITY("IPC Proposed Quantity"),
            CDO_OVERRIDE_REASON("CDO Override Reason"),
            BIZFIN_QUANTITY_RECOMMENDATION("BizFin Quantity Recommendation"),
            BIZFIN_COMMENTS("BizFin Comments");

            private String displayName;

            RequirementExcelHeaders(String displayName) {
                this.displayName = displayName;
            }


            private static final List<RequirementExcelHeaders> PROPOSED_LOCKED_HEADERS = Arrays.asList(REQUIREMENT_ID, WAREHOUSE, FSN, VERTICAL, CATEGORY, SUPER_CATEGORY,
                    TITLE, BRAND, PV_BAND, SALES_BAND, FLIPKART_SELLING_PRICE, SALES_BUCKET_0, SALES_BUCKET_1, SALES_BUCKET_2, SALES_BUCKET_3,
                    SALES_BUCKET_4, SALES_BUCKET_5,SALES_BUCKET_6,SALES_BUCKET_7, INVENTORY, INTRANSIT, FORECAST, QOH, PROCUREMENT_TYPE,
                    CURRENCY, PURCHASE_PRICE, MRP, SLA, TOTAL_VALUE, SUPPLIER, QUANTITY);
            private static final List<RequirementExcelHeaders> APPROVED_LOCKED_HEADERS = Arrays.asList(REQUIREMENT_ID, WAREHOUSE, FSN, VERTICAL, CATEGORY, SUPER_CATEGORY,
                    TITLE, BRAND, PV_BAND, SALES_BAND, FLIPKART_SELLING_PRICE, SALES_BUCKET_0, SALES_BUCKET_1, SALES_BUCKET_2, SALES_BUCKET_3,
                    SALES_BUCKET_4, SALES_BUCKET_5,SALES_BUCKET_6,SALES_BUCKET_7, INVENTORY, INTRANSIT, FORECAST, QOH, PROCUREMENT_TYPE,
                    CURRENCY, PURCHASE_PRICE, MRP, SLA, TOTAL_VALUE, SUPPLIER, QUANTITY, BIZFIN_QUANTITY_RECOMMENDATION, BIZFIN_COMMENTS);
            private static final List<RequirementExcelHeaders> BD_APPROVED_LOCKED_HEADERS = Arrays.asList(REQUIREMENT_ID, WAREHOUSE, FSN, VERTICAL, CATEGORY, SUPER_CATEGORY,
                    TITLE, BRAND, PV_BAND, SALES_BAND, FLIPKART_SELLING_PRICE, SALES_BUCKET_0, SALES_BUCKET_1, SALES_BUCKET_2, SALES_BUCKET_3,
                    SALES_BUCKET_4, SALES_BUCKET_5,SALES_BUCKET_6,SALES_BUCKET_7, INVENTORY, INTRANSIT, FORECAST, QOH, PROCUREMENT_TYPE,
                    CURRENCY, PURCHASE_PRICE, MRP, SLA, TOTAL_VALUE, SUPPLIER, QUANTITY, IPC_PROPOSED_QUANTITY, CDO_OVERRIDE_REASON);
            private static final List<RequirementExcelHeaders> BIZFIN_APPROVED_LOCKED_HEADERS = Arrays.asList(REQUIREMENT_ID, WAREHOUSE, FSN, VERTICAL, CATEGORY, SUPER_CATEGORY,
                    TITLE, BRAND, PV_BAND, SALES_BAND, FLIPKART_SELLING_PRICE, SALES_BUCKET_0, SALES_BUCKET_1, SALES_BUCKET_2, SALES_BUCKET_3,
                    SALES_BUCKET_4, SALES_BUCKET_5,SALES_BUCKET_6,SALES_BUCKET_7, INVENTORY, INTRANSIT, FORECAST, QOH, PROCUREMENT_TYPE,
                    CURRENCY, PURCHASE_PRICE, MRP, SLA, TOTAL_VALUE, SUPPLIER, QUANTITY, IPC_PROPOSED_QUANTITY, CDO_OVERRIDE_REASON, BIZFIN_QUANTITY_RECOMMENDATION, BIZFIN_COMMENTS);

            public static RequirementExcelHeaders fromString(String header) {
                for(RequirementExcelHeaders requirementExcelHeader : values()) {
                    if (requirementExcelHeader.displayName.equals(header)) {
                        return requirementExcelHeader;
                    }
                }
                return  null;
            }

            public static List<RequirementExcelHeaders> getLockedHeaders(String state) {
                RequirementApprovalState requirementApprovalState = RequirementApprovalState.fromString(state);
                switch(requirementApprovalState) {
                    case PRE_PROPOSED:
                        return PROPOSED_LOCKED_HEADERS;
                    case PROPOSED:
                        return PROPOSED_LOCKED_HEADERS;
                    case CDO_REVIEW:
                        return APPROVED_LOCKED_HEADERS;
                    case BIZFIN_REVIEW:
                        return BD_APPROVED_LOCKED_HEADERS;
                    case IPC_REVIEW:
                        return BIZFIN_APPROVED_LOCKED_HEADERS;
                    case IPC_FINALISED:
                        return BIZFIN_APPROVED_LOCKED_HEADERS;
                }
                return null;
            }



        }
