import Foundation

@objc public class MitekSdk: NSObject {

    @objc public static let errUnimplemented = "UNIMPLEMENTED"

    @objc public static func unimplementedResult(sessionType: String) -> [String: Any] {
        return [
            "success": false,
            "sessionType": sessionType,
            "errorCode": errUnimplemented,
            "errorMessage": "iOS implementation is not yet available.",
        ]
    }
}
