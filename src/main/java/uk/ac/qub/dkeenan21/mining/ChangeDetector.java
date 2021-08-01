package uk.ac.qub.dkeenan21.mining;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.filter.*;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.tinylog.Logger;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Detects changes in the version history of a Git repository
 */
public class ChangeDetector {
	private final Repository repository;

	/**
	 * Constructor which accepts a path to a repository
	 *
	 * @param repositoryPath a path to the repository
	 */
	public ChangeDetector(String repositoryPath) {
		this.repository = new RepositoryHelper().convertToRepository(repositoryPath);
	}

	/**
	 * Extracts all non-merge commits from the repository
	 *
	 * @return the non-merge commits
	 */
	public List<RevCommit> extractNonMergeCommits() {
		try {
			final ObjectId head = repository.resolve(Constants.HEAD);
			final Iterable<RevCommit> commits = new Git(repository).log().add(head).call();
			final List<RevCommit> nonMergeCommits = StreamSupport.stream(commits.spliterator(), false)
					.filter(commit -> commit.getParentCount() < 2)
					.collect(Collectors.toList());
			Collections.reverse(nonMergeCommits);
			Logger.debug("Extracted " + nonMergeCommits.size() + " non-merge commits");
			return nonMergeCommits;
		} catch (Exception exception) {
			Logger.error("An error occurred while extracting the non-merge commits from the repository");
			exception.printStackTrace();
			System.exit(1);
			return null;
		}
	}

	/**
	 * Generates a map representing a summary of the changes in a change period
	 *
	 * @param startCommitId the ID of the first commit in the change period
	 * @param endCommitId   the ID of the last commit in the change period
	 * @return a map containing an entry for each changed file in the change period
	 * entries are of the form [key = path, value = number of changed lines]
	 */
	public Map<String, Integer> summariseChanges(String startCommitId, String endCommitId) {
		try {
			validateCommitOrder(startCommitId, endCommitId);
			final Iterable<RevCommit> commits = extractNonMergeCommits(startCommitId, endCommitId);
			final Map<String, Integer> changePeriodSummary = new TreeMap<>();
			for (RevCommit commit : commits) {
				Logger.debug("Listing changes in commit " + commit.getName());
				final Iterable<DiffEntry> fileChanges = extractFileChanges(commit);
				for (DiffEntry fileChange : fileChanges) {
					// a value of '/dev/null' indicates file addition/deletion for an old/new path respectively
					final String changedFilePath = fileChange.getOldPath().equals("/dev/null") ? fileChange.getNewPath()
							: fileChange.getOldPath();
					final int numberOfChangedLinesInChangedFile = countChangedLines(fileChange);
					Logger.debug("– " + changedFilePath + " (" + numberOfChangedLinesInChangedFile + " lines)");
					if (changePeriodSummary.containsKey(changedFilePath)) {
						changePeriodSummary.put(changedFilePath, changePeriodSummary.get(changedFilePath)
								+ numberOfChangedLinesInChangedFile);
					} else {
						changePeriodSummary.put(changedFilePath, numberOfChangedLinesInChangedFile);
					}
				}
			}
			logChangePeriodSummary(changePeriodSummary);
			return changePeriodSummary;
		} catch (Exception exception) {
			Logger.error("An error occurred while summarising the change period");
			exception.printStackTrace();
			System.exit(1);
			return null;
		}
	}

	/**
	 * Extracts the non-merge commits from a change period
	 *
	 * @param startCommitId the ID of the first commit in the change period
	 * @param endCommitId   the ID of the last commit in the change period
	 * @return the extracted non-merge commits
	 */
	private Iterable<RevCommit> extractNonMergeCommits(String startCommitId, String endCommitId) {
		try {
			final RevCommit startCommit = repository.parseCommit(ObjectId.fromString(startCommitId));
			final RevCommit endCommit = repository.parseCommit(ObjectId.fromString(endCommitId));
			final Iterable<RevCommit> commits = new Git(repository).log().addRange(startCommit, endCommit).call();
			final List<RevCommit> nonMergeCommits = StreamSupport.stream(commits.spliterator(), false)
					.filter(commit -> commit.getParentCount() < 2)
					.collect(Collectors.toList());
			// must add startCommit separately, as it is not included in the 'since..until' range of log command
			if (startCommit.getParentCount() < 2) {
				nonMergeCommits.add(startCommit);
			}
			Collections.reverse(nonMergeCommits);
			return nonMergeCommits;
		} catch (Exception exception) {
			Logger.error("An error occurred while extracting the non-merge commits from a change period");
			exception.printStackTrace();
			System.exit(1);
			return null;
		}
	}

	/**
	 * Extracts the file changes from a commit
	 *
	 * @param commit the commit
	 * @return the file changes
	 */
	private Iterable<DiffEntry> extractFileChanges(RevCommit commit) {
		final List<DiffEntry> fileChanges;
		try (final DiffFormatter diffFormatter = generateDiffFormatter()) {
			// extract file changes by comparing the commit tree with that of its parent
			final ObjectReader objectReader = repository.newObjectReader();
			final AbstractTreeIterator commitTreeIterator = new CanonicalTreeParser(null, objectReader, commit.getTree());
			final AbstractTreeIterator parentCommitTreeIterator;
			if (commit.getParentCount() > 0) {
				final RevWalk revWalk = new RevWalk(repository);
				final RevCommit parentCommit = revWalk.parseCommit(commit.getParent(0).getId());
				parentCommitTreeIterator = new CanonicalTreeParser(null, objectReader, parentCommit.getTree());
			} else {
				// this is the initial commit (no parent), so must compare with empty tree
				parentCommitTreeIterator = new EmptyTreeIterator();
			}

			// extract the file changes for all '.java' files
			diffFormatter.setPathFilter(PathSuffixFilter.create(".java"));
			fileChanges = diffFormatter.scan(parentCommitTreeIterator, commitTreeIterator);
		} catch (Exception exception) {
			Logger.error("An error occurred while extracting the file changes from a commit");
			exception.printStackTrace();
			System.exit(1);
			return null;
		}

		// JGit path filters do not support filtering on glob patterns
		// also, negations of path suffix filters do not work (https://bugs.eclipse.org/bugs/show_bug.cgi?id=574253)
		// therefore, we must filter out test files using our own pattern-matching approach for now
		final String[] regexes = new String[]{"test/", "tests/", "tester/", "testers/", "androidTest/",
				"Test.java", "Tests.java", "Tester.java", "Testers.java"};
		final Set<Pattern> exclusionPatterns = new HashSet<>();
		for (String regex : regexes) {
			exclusionPatterns.add(Pattern.compile(regex));
		}

		// remove test files from consideration by testing each file path against the defined exclusion patterns
		final Iterator<DiffEntry> iterator = fileChanges.iterator();
		while (iterator.hasNext()) {
			final DiffEntry fileChange = iterator.next();
			for (Pattern exclusionPattern : exclusionPatterns) {
				if (exclusionPattern.matcher(fileChange.getOldPath()).find() ||
						exclusionPattern.matcher(fileChange.getNewPath()).find()) {
					iterator.remove();
					break;
				}
			}
		}

		return fileChanges;
	}

	/**
	 * Counts the changed lines in a file change
	 *
	 * @param fileChange the file change
	 * @return the number of changed lines in the file change
	 */
	private int countChangedLines(DiffEntry fileChange) {
		try (final DiffFormatter diffFormatter = generateDiffFormatter()) {
			final Iterable<Edit> changedRegionsOfFile = diffFormatter.toFileHeader(fileChange).toEditList();
			int numberOfChangedLines = 0;
			for (Edit changedRegionOfFile : changedRegionsOfFile) {
				if (changedRegionOfFile.getType() == Edit.Type.INSERT) {
					numberOfChangedLines += changedRegionOfFile.getLengthB();
				} else if (changedRegionOfFile.getType() == Edit.Type.DELETE) {
					numberOfChangedLines += changedRegionOfFile.getLengthA();
				} else if (changedRegionOfFile.getType() == Edit.Type.REPLACE) {
					numberOfChangedLines += changedRegionOfFile.getLengthA() + changedRegionOfFile.getLengthB();
				}
			}
			return numberOfChangedLines;
		} catch (Exception exception) {
			Logger.error("An error occurred while counting the changed lines in a file change");
			exception.printStackTrace();
			System.exit(1);
			return -1;
		}
	}

	/**
	 * Validates the time order of two commits
	 *
	 * @param startCommitId the ID of the commit which should have an earlier timestamp
	 * @param endCommitId   the ID of the commit which should have a later timestamp
	 */
	private void validateCommitOrder(String startCommitId, String endCommitId) {
		try {
			final RevCommit startCommit = repository.parseCommit(ObjectId.fromString(startCommitId));
			final RevCommit endCommit = repository.parseCommit(ObjectId.fromString(endCommitId));
			if (startCommit.getCommitTime() > endCommit.getCommitTime()) {
				Logger.error("Start commit time cannot be later than end commit time");
				System.exit(1);
			}
		} catch (Exception exception) {
			Logger.error("An error occurred while validating the order of start commit and end commit");
			exception.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Logs summary information about the changes in a change period
	 *
	 * @param changePeriodSummary a map containing an entry for each changed file in the change period
	 *                            entries are of the form [key = path, value = number of changed lines]
	 */
	private void logChangePeriodSummary(Map<String, Integer> changePeriodSummary) {
		final int numberOfChangedLinesInChangePeriod = changePeriodSummary.values().stream().reduce(0, Integer::sum);
		Logger.debug("Listing changes over all commits in change period");
		for (Map.Entry<String, Integer> entry : changePeriodSummary.entrySet()) {
			final String changedFilePath = entry.getKey();
			final int numberOfChangedLinesInChangedFile = entry.getValue();
			Logger.debug("– " + changedFilePath + " (" + numberOfChangedLinesInChangedFile + " lines)");
		}
		Logger.debug("Summary of changes in change period");
		Logger.debug("– Number of changed lines = " + numberOfChangedLinesInChangePeriod);
		Logger.debug("– Number of changed files = " + changePeriodSummary.size());
	}

	/**
	 * Creates and configures a diff formatter
	 *
	 * @return the diff formatter
	 */
	private DiffFormatter generateDiffFormatter() {
		final DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE);
		diffFormatter.setRepository(repository);
		return diffFormatter;
	}
}
