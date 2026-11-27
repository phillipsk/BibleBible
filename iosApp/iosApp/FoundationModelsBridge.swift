import Foundation
import ComposeApp

#if canImport(FoundationModels)
import FoundationModels

@available(iOS 26.0, *)
@Generable
private struct ScriptureTopicIntent {
    @Guide(description: "1 to 4 topic names selected strictly from the provided allowed topics list.")
    var topics: [String]
}
#endif

enum FoundationModelsBridge {
    static func register() {
        MainViewControllerKt.registerIosOnDeviceAiBridge(
            isAvailable: {
                #if canImport(FoundationModels)
                if #available(iOS 26.0, *) {
                    return KotlinBoolean(bool: SystemLanguageModel.default.isAvailable)
                }
                #endif
                return KotlinBoolean(bool: false)
            },
            extractTopics: { query, allowedCsv, onResult in
                #if canImport(FoundationModels)
                if #available(iOS 26.0, *) {
                    Task {
                        do {
                            guard SystemLanguageModel.default.isAvailable else {
                                onResult(nil)
                                return
                            }
                            let session = LanguageModelSession(
                                instructions: "You extract Bible study topic tags strictly from the allowed list. Never quote or invent scripture verses."
                            )
                            let prompt = "Map the user query '\(query)' to 1 to 4 topics chosen strictly from: \(allowedCsv)."
                            let response = try await session.respond(to: prompt, generating: ScriptureTopicIntent.self)
                            let joined = response.content.topics.joined(separator: ",")
                            onResult(joined.isEmpty ? nil : joined)
                        } catch {
                            onResult(nil)
                        }
                    }
                    return
                }
                #endif
                onResult(nil)
            },
            generateReflection: { reference, excerpt, onResult in
                #if canImport(FoundationModels)
                if #available(iOS 26.0, *) {
                    Task {
                        do {
                            guard SystemLanguageModel.default.isAvailable else {
                                onResult(nil)
                                return
                            }
                            let session = LanguageModelSession(
                                instructions: "Write one calm, practical reflection sentence grounded strictly in the supplied passage. Never invent other verses or use em-dashes or double hyphens."
                            )
                            let prompt = "Passage (\(reference)): \(excerpt)"
                            let response = try await session.respond(to: prompt)
                            let text = response.content.trimmingCharacters(in: .whitespacesAndNewlines)
                            onResult(text.isEmpty ? nil : text)
                        } catch {
                            onResult(nil)
                        }
                    }
                    return
                }
                #endif
                onResult(nil)
            }
        )
    }
}
